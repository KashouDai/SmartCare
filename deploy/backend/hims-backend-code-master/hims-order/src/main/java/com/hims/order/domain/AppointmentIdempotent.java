package com.hims.order.domain;

import lombok.Data;

import java.util.Date;

/**
 * 预约挂号幂等请求记录
 */
@Data
public class AppointmentIdempotent {
    /** 登录用户ID */
    private Long userId;
    /** 客户端幂等请求ID */
    private String requestId;
    /** 业务类型 */
    private String businessType;
    /** 请求指纹 */
    private String requestFingerprint;
    /** 成功后的预约ID */
    private Long appointmentsId;
    /** 成功后的订单ID */
    private Long orderId;
    /** 结果状态 */
    private String resultStatus;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
