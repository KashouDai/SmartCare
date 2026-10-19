package com.hims.order.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hims.common.annotation.Excel;
import com.hims.common.core.domain.BaseEntity;

/**
 * 订单对象 order
 */
public class Order extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单id */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    /** 预约id */
    @Excel(name = "预约id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long appointmentsId;

    /** 用户id */
    @Excel(name = "用户id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 订单金额 */
    @Excel(name = "订单金额")
    private BigDecimal orderAmount;

    /** 支付状态 */
    @Excel(name = "支付状态")
    private String payState;

    /** 支付截止时间 */
    private Date paymentDeadline;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public Long getOrderId()
    {
        return orderId;
    }
    public void setAppointmentsId(Long appointmentsId)
    {
        this.appointmentsId = appointmentsId;
    }

    public Long getAppointmentsId()
    {
        return appointmentsId;
    }
    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }
    public void setOrderAmount(BigDecimal orderAmount)
    {
        this.orderAmount = orderAmount;
    }

    public BigDecimal getOrderAmount()
    {
        return orderAmount;
    }
    public void setPayState(String payState)
    {
        this.payState = payState;
    }

    public String getPayState()
    {
        return payState;
    }

    public void setPaymentDeadline(Date paymentDeadline)
    {
        this.paymentDeadline = paymentDeadline;
    }

    public Date getPaymentDeadline()
    {
        return paymentDeadline;
    }
    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("orderId", getOrderId())
            .append("appointmentsId", getAppointmentsId())
            .append("userId", getUserId())
            .append("orderAmount", getOrderAmount())
            .append("payState", getPayState())
            .append("paymentDeadline", getPaymentDeadline())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
