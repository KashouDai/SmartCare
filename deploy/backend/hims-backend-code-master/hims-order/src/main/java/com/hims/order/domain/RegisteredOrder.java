package com.hims.order.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.hims.common.annotation.Excel;
import com.hims.common.core.domain.BaseEntity;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 挂号订单
 **/
@Data
public class RegisteredOrder extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 订单id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    /** 订单金额 */
    @Excel(name = "订单金额")
    private BigDecimal orderAmount;

    /** 支付状态 */
    @Excel(name = "支付状态")
    private String payState;

    /**
     * 医生姓名
     */
    private String doctorName;


    /** 预约时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "预约时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date dutyDate;

    /** 预约时段 */
    @Excel(name = "预约时段")
    private String dutyPeriod;

    /**
     * 医院名称
     */
    private String hospitalName;

    /**
     * 所属科室
     */
    private String deptName;

}
