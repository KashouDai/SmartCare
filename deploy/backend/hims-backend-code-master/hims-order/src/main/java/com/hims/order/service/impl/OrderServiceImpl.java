package com.hims.order.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.hims.order.domain.AppointmentIdempotent;
import com.hims.order.domain.Order;
import com.hims.order.domain.RegisteredOrder;
import com.hims.order.domain.RegisteredOrderRequest;
import com.hims.order.domain.RegisteredOrderResult;
import com.hims.order.mapper.AppointmentIdempotentMapper;
import com.hims.order.mapper.OrderMapper;
import com.hims.order.service.IOrderService;
import com.hims.common.exception.ServiceException;
import com.hims.common.core.redis.RedisCache;
import com.hims.common.utils.DateUtils;
import com.hims.common.utils.SecurityUtils;
import com.hims.common.utils.uuid.SnowflakeIdUtils;
import com.hims.common.constant.Constants;
import com.hims.info.domain.AppointmentsInfo;
import com.hims.info.domain.DutyInfo;
import com.hims.info.domain.PatientsInfo;
import com.hims.info.mapper.AppointmentsInfoMapper;
import com.hims.info.mapper.DutyInfoMapper;
import com.hims.info.mapper.PatientsInfoMapper;
import com.hims.info.mapper.UserPatientRelationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 订单Service业务层处理
 */
@Service
public class OrderServiceImpl implements IOrderService
{
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    /** 统一下单业务类型 */
    private static final String BUSINESS_TYPE_REGISTERED_ORDER = "REGISTERED_ORDER";

    /** 幂等记录处理中：仅存在于未提交事务内，事务提交前必被更新为成功，事务回滚则一并回滚 */
    private static final String IDEMPOTENT_RESULT_PROCESSING = "PROCESSING";

    /** 幂等记录成功 */
    private static final String IDEMPOTENT_RESULT_SUCCESS = "SUCCESS";

    /** 有效预约唯一索引名。appointments_info 上还有主键等其它唯一约束，须按索引名区分冲突来源 */
    private static final String ACTIVE_RESERVATION_INDEX = "uk_active_schedule_patient";

    /** Redis 快速防重标记 TTL；database幂等记录不受该 TTL 影响。 */
    private static final long REDIS_DEDUP_TTL_SECONDS = 60L;

    private static final String REDIS_DEDUP_KEY_PREFIX = "hospital:order:dedup:";

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private AppointmentIdempotentMapper appointmentIdempotentMapper;

    @Autowired
    private AppointmentsInfoMapper appointmentsInfoMapper;

    @Autowired
    private DutyInfoMapper dutyInfoMapper;

    @Autowired
    private PatientsInfoMapper patientsInfoMapper;

    @Autowired
    private UserPatientRelationMapper userPatientRelationMapper;

    /** 测试环境可不提供 Redis；缺失或异常时直接回退database幂等链路。 */
    @Autowired(required = false)
    private RedisCache redisCache;

    /**
     * 查询订单
     *
     * @param orderId 订单主键
     * @return 订单
     */
    @Override
    public Order selectOrderByOrderId(Long orderId)
    {
        return orderMapper.selectOrderByOrderId(orderId);
    }

    /**
     * 查询订单列表
     *
     * @param order 订单
     * @return 订单
     */
    @Override
    public List<Order> selectOrderList(Order order)
    {
        return orderMapper.selectOrderList(order);
    }

    /**
     * 新增订单（管理端入口）
     *
     * 金额与归属一律由服务端按预约所属排班推导，客户端传入的 orderAmount / userId 被忽略：
     * 该接口对管理端开放，但仍不应允许任意改价或把订单挂到他人名下。
     */
    @Override
    public String insertOrder(Order order)
    {
        if (order == null || order.getAppointmentsId() == null) {
            throw new ServiceException("订单参数不合法");
        }
        AppointmentsInfo appointment =
                appointmentsInfoMapper.selectAppointmentsInfoByAppointmentsId(order.getAppointmentsId());
        if (appointment == null) {
            throw new ServiceException("预约不存在");
        }
        DutyInfo duty = dutyInfoMapper.selectDutyInfoByScheduleId(appointment.getScheduleId());
        if (duty == null || duty.getRegistrationFee() == null) {
            throw new ServiceException("排班不存在或不可用");
        }
        order.setUserId(SecurityUtils.getUserId());
        order.setOrderAmount(duty.getRegistrationFee());
        order.setCreateTime(DateUtils.getNowDate());
        order.setOrderId(new SnowflakeIdUtils(5,8).nextId());
        orderMapper.insertOrder(order);
        return order.getOrderId()+"";
    }

    /**
     * 修改订单
     *
     * @param order 订单
     * @return 结果
     */
    @Override
    public int updateOrder(Order order)
    {
        order.setUpdateTime(DateUtils.getNowDate());
        return orderMapper.updateOrder(order);
    }

    /**
     * 批量删除订单
     *
     * @param orderIds 需要删除的订单主键
     * @return 结果
     */
    @Override
    public int deleteOrderByOrderIds(Long[] orderIds)
    {
        return orderMapper.deleteOrderByOrderIds(orderIds);
    }

    /**
     * 删除订单信息
     *
     * @param orderId 订单主键
     * @return 结果
     */
    @Override
    public int deleteOrderByOrderId(Long orderId)
    {
        return orderMapper.deleteOrderByOrderId(orderId);
    }

    /**
     * 根据用户id获取订单列表
     */
    @Override
    public List<RegisteredOrder> selectRegisteredOrderListByUserId(Order order) {
        return orderMapper.selectRegisteredOrderListByUserId(order);
    }

    @Override
    public void updateOrderStateById(String outTradeNo, String orderStatePaid) {
        orderMapper.updateOrderStateById(outTradeNo, orderStatePaid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisteredOrderResult createRegisteredOrder(RegisteredOrderRequest request) {
        validateRequest(request);
        Long userId = SecurityUtils.getUserId();
        Long patientId = request.getPatientId();
        Long scheduleId = request.getScheduleId();
        String requestId = request.getRequestId();
        String fingerprint = fingerprint(scheduleId, patientId);

        RegisteredOrderResult fastReplay = tryFastReplay(userId, requestId, fingerprint);
        if (fastReplay != null) {
            return fastReplay;
        }

        // 抢占幂等槽位：必须是事务内第一条database语句。一致性读快照在第一条普通读时才建立，
        // 此处先写不读，可保证并发撞键后的事务内查询读到已提交的仲裁结果，无需 FOR UPDATE。
        AppointmentIdempotent claim = new AppointmentIdempotent();
        claim.setUserId(userId);
        claim.setRequestId(requestId);
        claim.setBusinessType(BUSINESS_TYPE_REGISTERED_ORDER);
        claim.setRequestFingerprint(fingerprint);
        claim.setResultStatus(IDEMPOTENT_RESULT_PROCESSING);
        claim.setCreateTime(DateUtils.getNowDate());
        claim.setUpdateTime(DateUtils.getNowDate());
        try {
            appointmentIdempotentMapper.insertIdempotent(claim);
        } catch (DuplicateKeyException e) {
            // (user_id, request_id) 唯一约束仲裁：同一请求已被处理，读取并返回其已提交结果
            AppointmentIdempotent existing =
                    appointmentIdempotentMapper.selectByUserAndRequestForUpdate(userId, requestId);
            if (existing == null) {
                throw new ServiceException("预约请求正在处理中，请稍后重试");
            }
            return replay(existing, fingerprint, requestId);
        }

        PatientsInfo patient = patientsInfoMapper.selectPatientsInfoByPatientId(patientId);
        if (patient == null || !"0".equals(patient.getDelFlag())) {
            throw new ServiceException("患者不存在或已删除");
        }
        if (!userPatientRelationMapper.existsRelation(userId, patientId)) {
            throw new ServiceException("无权为该患者预约");
        }

        DutyInfo duty = dutyInfoMapper.selectDutyInfoByScheduleId(scheduleId);
        if (duty == null || !"0".equals(duty.getDelFlag())) {
            throw new ServiceException("排班不存在或不可用");
        }
        if (duty.getDutyDate() != null
                && DateUtil.compare(DateUtil.beginOfDay(duty.getDutyDate()), DateUtil.beginOfDay(DateUtils.getNowDate())) < 0) {
            throw new ServiceException("排班已过期");
        }
        if (duty.getAppointmentsNum() == null || duty.getAppointmentsNum() <= 0
                || duty.getRegistrationFee() == null || duty.getRegistrationFee().signum() < 0) {
            throw new ServiceException("排班不存在或不可用");
        }

        // 有效预约预检查：仅用于给出明确错误，最终由 (schedule_id, active_patient_id) 唯一索引仲裁
        if (appointmentsInfoMapper.countActiveByScheduleAndPatient(scheduleId, patientId) > 0) {
            throw new ServiceException("该患者已预约该排班");
        }

        if (dutyInfoMapper.increaseAppointmentsNumMade(scheduleId) != 1) {
            throw new ServiceException("号源已满");
        }

        AppointmentsInfo appointment = new AppointmentsInfo();
        appointment.setAppointmentsId(new SnowflakeIdUtils(5, 8).nextId());
        appointment.setScheduleId(scheduleId);
        appointment.setDoctorId(duty.getDoctorId());
        appointment.setPatientId(patientId);
        appointment.setStatus(Constants.ORDER_STATE_PENDING_PAYMENT);
        appointment.setCreateTime(DateUtils.getNowDate());
        try {
            if (appointmentsInfoMapper.insertAppointmentsInfo(appointment) != 1) {
                throw new ServiceException("预约创建失败，请稍后重试");
            }
        } catch (DuplicateKeyException e) {
            // 并发下不同 requestId 争抢同一患者同一排班，由有效预约唯一索引兜底仲裁。
            // 只有该索引的冲突才是「重复预约」，主键冲突等其它唯一约束冲突不能误报给用户。
            if (isActiveReservationConflict(e)) {
                throw new ServiceException("该患者已预约该排班");
            }
            throw new ServiceException("预约创建失败，请稍后重试");
        }

        Order order = new Order();
        order.setOrderId(new SnowflakeIdUtils(5, 8).nextId());
        order.setAppointmentsId(appointment.getAppointmentsId());
        order.setUserId(userId);
        order.setOrderAmount(duty.getRegistrationFee());
        order.setPayState(Constants.ORDER_STATE_PENDING_PAYMENT);
        order.setCreateTime(DateUtils.getNowDate());
        order.setPaymentDeadline(DateUtil.offsetMinute(order.getCreateTime(), 15));
        if (orderMapper.insertOrder(order) != 1) {
            throw new ServiceException("订单创建失败，请稍后重试");
        }

        appointmentIdempotentMapper.updateResult(userId, requestId, appointment.getAppointmentsId(),
                order.getOrderId(), IDEMPOTENT_RESULT_SUCCESS);

        RegisteredOrderResult result = new RegisteredOrderResult();
        result.setOrderId(order.getOrderId());
        result.setAppointmentsId(appointment.getAppointmentsId());
        result.setOrderAmount(order.getOrderAmount());
        result.setPayState(order.getPayState());
        result.setRequestId(requestId);
        cacheSuccessfulRequestAfterCommit(userId, requestId);
        return result;
    }

    /**
     * Redis 只负责成功请求的短期快速拦截，命中后仍回查database，避免缓存成为事实源。
     * Redis 不可用、标记过期或database记录不存在时均回退正常事务链路。
     */
    private RegisteredOrderResult tryFastReplay(Long userId, String requestId, String fingerprint) {
        String key = redisDedupKey(userId, requestId);
        try {
            if (redisCache == null || !Boolean.TRUE.equals(redisCache.hasKey(key))) {
                return null;
            }
            AppointmentIdempotent existing = appointmentIdempotentMapper.selectByUserAndRequest(userId, requestId);
            if (existing == null) {
                redisCache.deleteObject(key);
                return null;
            }
            return replay(existing, fingerprint, requestId);
        } catch (RuntimeException ignored) {
            // Redis 仅是优化组件，任何连接、序列化或缓存异常都不能阻断database保障链路。
            return null;
        }
    }

    private void cacheSuccessfulRequestAfterCommit(final Long userId, final String requestId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    cacheSuccessfulRequest(userId, requestId);
                }
            });
            return;
        }
        cacheSuccessfulRequest(userId, requestId);
    }

    private void cacheSuccessfulRequest(Long userId, String requestId) {
        try {
            if (redisCache != null) {
                redisCache.setCacheObjectIfAbsent(redisDedupKey(userId, requestId), "1",
                        REDIS_DEDUP_TTL_SECONDS, TimeUnit.SECONDS);
            }
        } catch (RuntimeException ignored) {
            // Redis 写入失败不影响已经提交的预约结果。
        }
    }

    private String redisDedupKey(Long userId, String requestId) {
        return REDIS_DEDUP_KEY_PREFIX + userId + ":" + requestId;
    }

    /**
     * 计算请求指纹，覆盖业务类型、排班与患者
     */
    private String fingerprint(Long scheduleId, Long patientId) {
        return DigestUtil.sha256Hex(BUSINESS_TYPE_REGISTERED_ORDER + "|" + scheduleId + "|" + patientId);
    }

    /**
     * 处理命中幂等记录的请求：指纹一致且已成功时返回原结果，指纹不同时拒绝。
     * 返回的金额与支付状态取自订单当前值，而非首次下单时的快照。
     */
    private RegisteredOrderResult replay(AppointmentIdempotent record, String fingerprint, String requestId) {
        if (!fingerprint.equals(record.getRequestFingerprint())) {
            throw new ServiceException("该请求编号已被其他参数使用，请勿复用");
        }
        if (!IDEMPOTENT_RESULT_SUCCESS.equals(record.getResultStatus()) || record.getOrderId() == null) {
            throw new ServiceException("预约请求正在处理中，请稍后重试");
        }
        Order order = orderMapper.selectOrderByOrderId(record.getOrderId());
        if (order == null) {
            throw new ServiceException("预约结果不可用，请稍后重试");
        }
        RegisteredOrderResult result = new RegisteredOrderResult();
        result.setOrderId(record.getOrderId());
        result.setAppointmentsId(record.getAppointmentsId());
        result.setOrderAmount(order.getOrderAmount());
        result.setPayState(order.getPayState());
        result.setRequestId(requestId);
        return result;
    }

    /**
     * 判断唯一约束冲突是否来自有效预约唯一索引。
     * appointments_info 上除 uk_active_schedule_patient 外还有主键 (appointments_id) 等唯一约束，
     * 其冲突与「该患者已预约该排班」无关，必须按系统错误处理。
     * 判定依据异常链消息中的索引名；MySQL 5.7/8 与 H2 的消息均包含该索引名，大小写不敏感匹配。
     * 匹配不到时按系统错误处理：宁可漏报「已预约」，也不误报。
     */
    private boolean isActiveReservationConflict(DuplicateKeyException e) {
        Throwable cause = e;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ENGLISH).contains(ACTIVE_RESERVATION_INDEX)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void validateRequest(RegisteredOrderRequest request) {
        if (request == null || request.getScheduleId() == null || request.getScheduleId() <= 0
                || request.getPatientId() == null || request.getPatientId() <= 0) {
            throw new ServiceException("预约参数不合法");
        }
        String requestId = request.getRequestId();
        if (requestId == null || !REQUEST_ID_PATTERN.matcher(requestId).matches()) {
            throw new ServiceException("预约参数不合法");
        }
    }



}
