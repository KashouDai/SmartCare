package com.hims.order.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.easysdk.factory.Factory;
import com.hims.common.annotation.Anonymous;
import com.hims.common.core.domain.R;
import com.hims.order.config.AliPayConfig;
import com.hims.order.domain.AliPay;
import com.hims.order.domain.PayRecord;
import com.hims.order.service.IOrderService;
import com.hims.order.service.IPayRecordService;
import com.hims.order.service.OrderLifecycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付Controller
 */
@RestController
@RequestMapping("/order/alipay")
@Transactional(rollbackFor = Exception.class)
public class AliPayController {

   private static final String GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
   private static final String FORMAT = "JSON";
   private static final String CHARSET = "utf-8";
   private static final String SIGN_TYPE = "RSA2";
   @Resource
   private AliPayConfig aliPayConfig;
   @Autowired
   private IOrderService orderService;
   @Autowired
   private OrderLifecycleService orderLifecycleService;
   @Autowired
   private IPayRecordService payRecordService;

   @GetMapping("/pay") // 前端路径参数格式?subject=xxx&traceNo=xxx&totalAmount=xxx
   @Anonymous
   public void pay(AliPay aliPay, HttpServletResponse httpResponse) throws Exception {
      // 1. 创建Client，通用SDK提供的Client，负责调用支付宝的API
      AlipayClient alipayClient = new DefaultAlipayClient(GATEWAY_URL, aliPayConfig.getAppId(),
              aliPayConfig.getAppPrivateKey(), FORMAT, CHARSET, aliPayConfig.getAlipayPublicKey(), SIGN_TYPE);

      // 2. 创建 Request并设置Request参数
      AlipayTradePagePayRequest request = new AlipayTradePagePayRequest(); // 发送请求的 Request类
      request.setNotifyUrl(aliPayConfig.getNotifyUrl());
      request.setReturnUrl(aliPayConfig.getReturnUrl());
      request.setBizContent("{\"out_trade_no\":\"" + aliPay.getTraceNo() + "\"," // 我们自己生成的订单编号
              + "\"total_amount\":\"" + aliPay.getTotalAmount() + "\","  // 订单的总金额
              + "\"subject\":\"" + aliPay.getSubject() + "\","  // 支付的名称
              + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");  // 固定配置

      // 执行请求，拿到响应的结果，返回给浏览器
      String form = "";
      try {
         // 调用SDK生成表单
         form = alipayClient.pageExecute(request, "POST").getBody();
         form = form.replace("http://openapi-sandbox.dl.alipaydev.com/gateway.do", GATEWAY_URL);
      } catch (AlipayApiException e) {
         e.printStackTrace();
      }
      httpResponse.setContentType("text/html;charset=" + CHARSET);
      // 直接将完整的表单html输出到页面
      httpResponse.getWriter().write(form);
      httpResponse.getWriter().flush();
      httpResponse.getWriter().close();
   }

   @PostMapping("/notify")  // 注意这里必须是POST接口
   @Anonymous
   public String payNotify(HttpServletRequest request) throws Exception {
      if ("TRADE_SUCCESS".equals(request.getParameter("trade_status"))) {
         System.out.println("=========支付宝异步回调========");

         Map<String, String> params = new HashMap<>();
         Map<String, String[]> requestParams = request.getParameterMap();
         for (String name : requestParams.keySet()) {
            params.put(name, request.getParameter(name));
         }

         String outTradeNo = params.get("out_trade_no");
         // 支付宝验签
         if (Factory.Payment.Common().verifyNotify(params)) {
            // 验签通过
            System.out.println("交易名称: " + params.get("subject"));
            System.out.println("交易状态: " + params.get("trade_status"));
            System.out.println("支付宝交易凭证号: " + params.get("trade_no"));
            System.out.println("商户订单号: " + params.get("out_trade_no"));
            System.out.println("交易金额: " + params.get("total_amount"));
            System.out.println("买家在支付宝唯一id: " + params.get("buyer_id"));
            System.out.println("买家付款时间: " + params.get("gmt_payment"));
            System.out.println("买家付款金额: " + params.get("buyer_pay_amount"));

            Long orderId = Long.valueOf(outTradeNo);
            com.hims.order.domain.Order order = orderService.selectOrderByOrderId(orderId);
            if (order == null || order.getOrderAmount() == null
                    || new BigDecimal(params.get("total_amount")).compareTo(order.getOrderAmount()) != 0) {
               return "fail";
            }
            if (!orderLifecycleService.markPaid(orderId)) {
               return "fail";
            }

            // 记录支付记录
            if (payRecordService.selectPayRecordByOutTradeNo(outTradeNo) != null) {
               return "success";
            }
            PayRecord payRecord = new PayRecord();
            payRecord.setSubject(params.get("subject"));
            payRecord.setTradeStatus(params.get("trade_status"));
            payRecord.setTradeNo(params.get("trade_no"));
            payRecord.setOutTradeNo(Long.valueOf(params.get("out_trade_no")));
            payRecord.setTotalAmount(new BigDecimal(params.get("total_amount")));
            payRecord.setGmtPayment(DateUtil.parse(params.get("gmt_payment")));
            payRecord.setBuyerPayAmount(new BigDecimal(params.get("buyer_pay_amount")));
            payRecordService.insertPayRecord(payRecord);
         }
      }
      return "success";
   }

   @GetMapping("/return/{orderId}")
   @Anonymous
   public R returnPay(@PathVariable String orderId) throws AlipayApiException {
      PayRecord payRecord = payRecordService.selectPayRecordByOutTradeNo(orderId);
      if (payRecord == null) {
         // 没有支付信息，无需退费
         orderLifecycleService.cancelPending(Long.valueOf(orderId));
         return R.ok();
      }

      // 1. 创建Client，通用SDK提供的Client，负责调用支付宝的API
      AlipayClient alipayClient = new DefaultAlipayClient(GATEWAY_URL,
              aliPayConfig.getAppId(), aliPayConfig.getAppPrivateKey(), FORMAT, CHARSET,
              aliPayConfig.getAlipayPublicKey(), SIGN_TYPE);
      // 2. 创建 Request，设置参数
      AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
      JSONObject bizContent = new JSONObject();
      bizContent.set("trade_no", payRecord.getTradeNo());  // 支付宝回调的订单流水号
      bizContent.set("refund_amount", payRecord.getTotalAmount());  // 订单的总金额
      bizContent.set("out_request_no", payRecord.getOutTradeNo());   //  我的订单编号

      // 返回参数选项，按需传入
      //JSONArray queryOptions = new JSONArray();
      //queryOptions.add("refund_detail_item_list");
      //bizContent.put("query_options", queryOptions);

      request.setBizContent(bizContent.toString());

      // 3. 执行请求
      AlipayTradeRefundResponse response = alipayClient.execute(request);
      if (response.isSuccess()) {  // 退款成功，isSuccess 为true
         System.out.println("调用成功");

         // 4. 更新database状态
         orderLifecycleService.refundPaid(Long.valueOf(orderId));
         return R.ok();
      } else {   // 退款失败，isSuccess 为false
         System.out.println(response.getBody());
         return R.fail(response.getCode(), response.getBody());
      }

   }
}
