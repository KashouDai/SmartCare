package com.hims.order.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hims.common.annotation.Excel;
import com.hims.common.core.domain.BaseEntity;

/**
 * 支付信息对象 pay_record
 */
@Data
public class PayRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 支付宝交易凭证号 */
    private String tradeNo;

    /** 交易名称 */
    @Excel(name = "交易名称")
    private String subject;

    /** 交易状态 */
    @Excel(name = "交易状态")
    private String tradeStatus;

    /** 商户订单号 */
    @Excel(name = "商户订单号")
    private Long outTradeNo;

    /** 交易金额 */
    @Excel(name = "交易金额")
    private BigDecimal totalAmount;

    /** 买家付款时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "买家付款时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date gmtPayment;

    /** 买家付款金额 */
    @Excel(name = "买家付款金额")
    private BigDecimal buyerPayAmount;

}
