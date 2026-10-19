package com.hims.order.service.impl;

import com.hims.common.core.domain.model.LoginUser;
import com.hims.common.core.redis.RedisCache;
import com.hims.common.exception.ServiceException;
import com.hims.order.domain.RegisteredOrderRequest;
import com.hims.order.domain.RegisteredOrderResult;
import com.hims.order.service.IOrderService;
import com.hims.order.support.RegisteredOrderTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 持久化幂等与有效预约database约束的端到端验证。
 *
 * 使用真实 H2 mapper（不使用 Mock），使幂等表主键 (user_id, request_id) 与
 * appointments_info 的有效预约唯一索引真正参与并发仲裁。
 * H2 1.4.200 的隔离级别为 READ COMMITTED，与生产 MySQL 5.7.18 的 REPEATABLE READ 不同，
 * 因此本类验证的是端到端业务语义，不能替代真实 MySQL 上的并发验证（见实施记录）。
 */
@SpringBootTest(classes = RegisteredOrderTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RegisteredOrderIdempotencyTest {

    private static final Long USER_ID = 100L;
    private static final Long PATIENT_ID = 200L;
    private static final Long OTHER_PATIENT_ID = 201L;
    private static final Long SCHEDULE_ID = 1L;

    @Autowired
    private IOrderService orderService;

    @MockBean
    private RedisCache redisCache;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        when(redisCache.hasKey(anyString())).thenReturn(false);
        jdbc.update("delete from order_info");
        jdbc.update("delete from appointments_info");
        jdbc.update("delete from appointment_idempotent");
        jdbc.update("delete from user_patient_rel");
        jdbc.update("delete from patients_info");
        jdbc.update("delete from duty_info");
        jdbc.update("insert into duty_info (schedule_id, doctor_id, duty_date, appointments_num, appointments_num_made, registration_fee) values (?, 10, ?, 5, 0, 100.00)",
                SCHEDULE_ID, Date.valueOf(LocalDate.now().plusDays(1)));
        jdbc.update("insert into patients_info (patient_id, nick_name) values (?, ?)", PATIENT_ID, "测试患者");
        jdbc.update("insert into patients_info (patient_id, nick_name) values (?, ?)", OTHER_PATIENT_ID, "他人患者");
        jdbc.update("insert into user_patient_rel (user_id, patient_id) values (?, ?)", USER_ID, PATIENT_ID);
        loginAs(USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 场景 1：正常下单 */
    @Test
    void 正常下单_一组记录且幂等保存成功() {
        RegisteredOrderResult result = orderService.createRegisteredOrder(
                request(SCHEDULE_ID, PATIENT_ID, "req-idem-normal-0001"));

        assertNotNull(result.getOrderId());
        assertNotNull(result.getAppointmentsId());
        assertEquals(1, count("select count(*) from appointments_info"));
        assertEquals(1, count("select count(*) from order_info"));
        assertEquals(1L, madeCount(SCHEDULE_ID));

        Map<String, Object> row = jdbc.queryForMap(
                "select appointments_id, order_id, result_status, request_fingerprint from appointment_idempotent where user_id = ? and request_id = ?",
                USER_ID, "req-idem-normal-0001");
        assertEquals("SUCCESS", row.get("result_status"));
        assertEquals(result.getAppointmentsId(), ((Number) row.get("appointments_id")).longValue());
        assertEquals(result.getOrderId(), ((Number) row.get("order_id")).longValue());
        assertNotNull(row.get("request_fingerprint"));
        verify(redisCache).setCacheObjectIfAbsent(anyString(), any(), anyLong(), any());
    }

    /** 场景 2：同一 requestId 串行重复 —— 返回原结果，号源只加一次 */
    @Test
    void 同一requestId串行重复_返回原结果且不额外占号() {
        RegisteredOrderResult first = orderService.createRegisteredOrder(
                request(SCHEDULE_ID, PATIENT_ID, "req-idem-serial-0001"));
        RegisteredOrderResult second = orderService.createRegisteredOrder(
                request(SCHEDULE_ID, PATIENT_ID, "req-idem-serial-0001"));

        assertEquals(first.getOrderId(), second.getOrderId());
        assertEquals(first.getAppointmentsId(), second.getAppointmentsId());
        assertEquals(first.getOrderAmount(), second.getOrderAmount());
        assertEquals(first.getPayState(), second.getPayState());
        assertEquals("req-idem-serial-0001", second.getRequestId());

        assertEquals(1, count("select count(*) from appointments_info"));
        assertEquals(1, count("select count(*) from order_info"));
        assertEquals(1, count("select count(*) from appointment_idempotent"));
        assertEquals(1L, madeCount(SCHEDULE_ID));
    }

    /** 场景 3：同一 requestId 并发重复 —— 最多一组记录，全部调用得到同一结果 */
    @Test
    void 同一requestId并发重复_最多一组记录() throws Exception {
        int threads = 20;
        String requestId = "req-idem-concurrent-0001";

        List<Attempt> attempts = runConcurrently(threads,
                () -> orderService.createRegisteredOrder(request(SCHEDULE_ID, PATIENT_ID, requestId)));

        List<RegisteredOrderResult> succeeded = new ArrayList<>();
        for (Attempt attempt : attempts) {
            if (attempt.error != null) {
                throw new AssertionError("同一 requestId 的并发重复请求不应失败：" + attempt.error.getMessage(), attempt.error);
            }
            succeeded.add(attempt.result);
        }
        assertEquals(threads, succeeded.size());

        Long expectedOrderId = succeeded.get(0).getOrderId();
        Long expectedAppointmentsId = succeeded.get(0).getAppointmentsId();
        for (RegisteredOrderResult result : succeeded) {
            assertEquals(expectedOrderId, result.getOrderId(), "并发重复请求必须返回同一订单");
            assertEquals(expectedAppointmentsId, result.getAppointmentsId(), "并发重复请求必须返回同一预约");
        }

        assertEquals(1, count("select count(*) from appointments_info"));
        assertEquals(1, count("select count(*) from order_info"));
        assertEquals(1, count("select count(*) from appointment_idempotent"));
        assertEquals(1L, madeCount(SCHEDULE_ID));
    }

    /** 场景 4：同一 requestId 不同参数 —— 明确冲突拒绝，不产生新记录、不额外占号 */
    @Test
    void 同一requestId不同参数_明确冲突拒绝() {
        RegisteredOrderResult first = orderService.createRegisteredOrder(
                request(SCHEDULE_ID, PATIENT_ID, "req-idem-conflict-0001"));

        ServiceException conflict = assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request(SCHEDULE_ID, OTHER_PATIENT_ID, "req-idem-conflict-0001")));
        assertTrue(conflict.getMessage().contains("请求编号"), "冲突错误必须明确指向 requestId 复用：" + conflict.getMessage());

        assertEquals(1, count("select count(*) from appointments_info"));
        assertEquals(1, count("select count(*) from order_info"));
        assertEquals(1, count("select count(*) from appointment_idempotent"));
        assertEquals(1L, madeCount(SCHEDULE_ID));

        RegisteredOrderResult replay = orderService.createRegisteredOrder(
                request(SCHEDULE_ID, PATIENT_ID, "req-idem-conflict-0001"));
        assertEquals(first.getOrderId(), replay.getOrderId());
    }

    /**
     * 场景 5：同一患者 + 同一排班、不同 requestId 并发 —— 最多一条有效预约、最多一个号源。
     * 号源容量（20）刻意大于线程数（20 个不同 requestId 各自抢一次），
     * 使原子占号不足以拦截，从而真正验证「有效预约唯一索引」是最终仲裁者。
     */
    @Test
    void 同患者同排班不同requestId并发_最多一条有效预约() throws Exception {
        jdbc.update("update duty_info set appointments_num = 20 where schedule_id = ?", SCHEDULE_ID);
        int threads = 20;

        List<Attempt> attempts = runConcurrently(threads, () -> orderService.createRegisteredOrder(
                request(SCHEDULE_ID, PATIENT_ID, "req-idem-dup-" + System.nanoTime())));

        int succeeded = 0;
        for (Attempt attempt : attempts) {
            if (attempt.error == null) {
                succeeded++;
            } else {
                assertTrue(attempt.error instanceof ServiceException,
                        "并发争抢同一患者同一排班应返回业务冲突而非系统错误：" + attempt.error);
                assertTrue(attempt.error.getMessage().contains("已预约"),
                        "冲突错误应说明该患者已预约该排班：" + attempt.error.getMessage());
            }
        }

        assertEquals(1, succeeded, "同一患者同一排班只能有一条有效预约");
        assertEquals(1, count("select count(*) from appointments_info where active_patient_id = " + PATIENT_ID));
        assertEquals(1, count("select count(*) from order_info"));
        assertEquals(1, count("select count(*) from appointment_idempotent"),
                "失败请求的幂等记录必须随事务回滚，不得残留");
        assertEquals(1L, madeCount(SCHEDULE_ID), "失败请求占用的号源必须随事务回滚");
    }

    /** 场景 8：号源耗尽 —— 无预约、无订单、无幂等残留 */
    @Test
    void 号源耗尽_无业务写入且无幂等残留() {
        jdbc.update("update duty_info set appointments_num = 1, appointments_num_made = 1 where schedule_id = ?", SCHEDULE_ID);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request(SCHEDULE_ID, PATIENT_ID, "req-idem-exhausted-0001")));
        assertTrue(exception.getMessage().contains("号源"), exception.getMessage());

        assertEquals(0, count("select count(*) from appointments_info"));
        assertEquals(0, count("select count(*) from order_info"));
        assertEquals(0, count("select count(*) from appointment_idempotent"));
        assertEquals(1L, madeCount(SCHEDULE_ID));
    }

    /** 场景 9：伪造患者归属 —— 拒绝且不写任何业务数据 */
    @Test
    void 伪造患者归属_拒绝且不写业务数据() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request(SCHEDULE_ID, OTHER_PATIENT_ID, "req-idem-forged-0001")));
        assertTrue(exception.getMessage().contains("无权"), exception.getMessage());

        assertEquals(0, count("select count(*) from appointments_info"));
        assertEquals(0, count("select count(*) from order_info"));
        assertEquals(0, count("select count(*) from appointment_idempotent"));
        assertEquals(0L, madeCount(SCHEDULE_ID));
    }

    /** 场景 10：首次失败后同一 requestId 可重试，不被残留「处理中」记录永久阻断 */
    @Test
    void 首次失败后同一requestId可重试() {
        jdbc.update("update duty_info set appointments_num = 1, appointments_num_made = 1 where schedule_id = ?", SCHEDULE_ID);
        String requestId = "req-idem-retry-0001";

        assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request(SCHEDULE_ID, PATIENT_ID, requestId)));
        assertEquals(0, count("select count(*) from appointment_idempotent"),
                "首次失败后不得残留幂等记录");

        jdbc.update("update duty_info set appointments_num = 5, appointments_num_made = 0 where schedule_id = ?", SCHEDULE_ID);

        RegisteredOrderResult result = orderService.createRegisteredOrder(
                request(SCHEDULE_ID, PATIENT_ID, requestId));
        assertNotNull(result.getOrderId());
        assertEquals(1, count("select count(*) from appointments_info"));
        assertEquals(1, count("select count(*) from order_info"));
        assertEquals(1, count("select count(*) from appointment_idempotent"));
        assertEquals(1L, madeCount(SCHEDULE_ID));
    }

    private RegisteredOrderRequest request(Long scheduleId, Long patientId, String requestId) {
        RegisteredOrderRequest request = new RegisteredOrderRequest();
        request.setScheduleId(scheduleId);
        request.setPatientId(patientId);
        request.setRequestId(requestId);
        return request;
    }

    private void loginAs(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    /**
     * 并发执行下单调用，每个线程使用独立的 SecurityContext 与独立事务。
     * 业务异常被捕获到 Attempt.error，不中断其他线程。
     */
    private List<Attempt> runConcurrently(int threads, Callable<RegisteredOrderResult> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Attempt>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                loginAs(USER_ID);
                try {
                    start.await();
                    return new Attempt(task.call(), null);
                } catch (Exception e) {
                    return new Attempt(null, e);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }
        start.countDown();

        List<Attempt> attempts = new ArrayList<>();
        for (Future<Attempt> future : futures) {
            attempts.add(future.get());
        }
        pool.shutdown();
        return attempts;
    }

    private long madeCount(Long scheduleId) {
        Long value = jdbc.queryForObject("select appointments_num_made from duty_info where schedule_id = ?", Long.class, scheduleId);
        return value == null ? 0L : value;
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private static class Attempt {
        private final RegisteredOrderResult result;
        private final Exception error;

        Attempt(RegisteredOrderResult result, Exception error) {
            this.result = result;
            this.error = error;
        }
    }
}
