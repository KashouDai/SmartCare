package com.hims.order.mapper;

import com.hims.order.domain.AppointmentIdempotent;
import org.apache.ibatis.annotations.Param;

/**
 * 预约挂号幂等请求记录 Mapper
 */
public interface AppointmentIdempotentMapper {

    /**
     * 按用户与请求ID查询幂等记录
     */
    AppointmentIdempotent selectByUserAndRequest(@Param("userId") Long userId, @Param("requestId") String requestId);

    /**
     * 按用户与请求ID加锁查询幂等记录，用于并发撞键后读取已提交的仲裁结果
     */
    AppointmentIdempotent selectByUserAndRequestForUpdate(@Param("userId") Long userId, @Param("requestId") String requestId);

    /**
     * 插入幂等记录，由主键 (user_id, request_id) 仲裁并发
     */
    int insertIdempotent(AppointmentIdempotent record);

    /**
     * 保存最终幂等结果
     */
    int updateResult(@Param("userId") Long userId, @Param("requestId") String requestId,
                     @Param("appointmentsId") Long appointmentsId, @Param("orderId") Long orderId,
                     @Param("resultStatus") String resultStatus);
}
