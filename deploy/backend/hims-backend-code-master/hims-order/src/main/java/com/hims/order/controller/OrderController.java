package com.hims.order.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.hims.order.domain.RegisteredOrder;
import com.hims.order.domain.RegisteredOrderRequest;
import com.hims.order.domain.RegisteredOrderResult;
import com.hims.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hims.common.annotation.Log;
import com.hims.common.core.controller.BaseController;
import com.hims.common.core.domain.AjaxResult;
import com.hims.common.enums.BusinessType;
import com.hims.order.domain.Order;
import com.hims.order.service.IOrderService;
import com.hims.common.utils.poi.ExcelUtil;
import com.hims.common.core.page.TableDataInfo;

/**
 * 订单Controller
 */
@RestController
@RequestMapping("/order/orderInfo")
public class OrderController extends BaseController
{
    @Autowired
    private IOrderService orderService;

    /**
     * 查询订单列表
     */
    @PreAuthorize("@ss.hasPermi('order:orderInfo:list')")
    @GetMapping("/list")
    public TableDataInfo list(Order order)
    {
        startPage();
        List<Order> list = orderService.selectOrderList(order);
        return getDataTable(list);
    }

    /**
     * 导出订单列表
     */
//    @PreAuthorize("@ss.hasPermi('order:orderInfo:export')")
    @Log(title = "订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Order order)
    {
        List<Order> list = orderService.selectOrderList(order);
        ExcelUtil<Order> util = new ExcelUtil<Order>(Order.class);
        util.exportExcel(response, list, "订单数据");
    }

    /**
     * 获取订单详细信息
     */
//    @PreAuthorize("@ss.hasPermi('order:orderInfo:query')")
    @GetMapping(value = "/{orderId}")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        return success(orderService.selectOrderByOrderId(orderId));
    }

    /**
     * 新增订单
     * 仅管理端可用：患者端下单一律走统一事务入口 POST /order/orderInfo/registeredOrder。
     * 金额与用户身份由服务端按预约所属排班推导，客户端传入值一律忽略。
     */
    @PreAuthorize("@ss.hasPermi('order:orderInfo:add')")
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping
    public R<String> add(@RequestBody Order order)
    {
        return R.ok(orderService.insertOrder(order));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/registeredOrder")
    public R<RegisteredOrderResult> registeredOrder(@RequestBody RegisteredOrderRequest request)
    {
        return R.ok(orderService.createRegisteredOrder(request));
    }

    /**
     * 修改订单
     */
//    @PreAuthorize("@ss.hasPermi('order:orderInfo:edit')")
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Order order)
    {
        return toAjax(orderService.updateOrder(order));
    }

    /**
     * 删除订单
     */
//    @PreAuthorize("@ss.hasPermi('order:orderInfo:remove')")
    @Log(title = "订单", businessType = BusinessType.DELETE)
	@DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(orderService.deleteOrderByOrderIds(orderIds));
    }


    /**
     * 根据用户id获取订单列表
     */
    @GetMapping("/getList")
    public TableDataInfo getList(Order order)
    {
        startPage();
        List<RegisteredOrder> list = orderService.selectRegisteredOrderListByUserId(order);
        return getDataTable(list);
    }
}
