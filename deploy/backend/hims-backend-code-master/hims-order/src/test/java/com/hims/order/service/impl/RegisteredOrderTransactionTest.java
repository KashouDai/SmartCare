package com.hims.order.service.impl;

import com.hims.common.core.domain.model.LoginUser;
import com.hims.common.exception.ServiceException;
import com.hims.order.domain.Order;
import com.hims.order.domain.RegisteredOrderRequest;
import com.hims.order.mapper.OrderMapper;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = RegisteredOrderTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RegisteredOrderTransactionTest {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private DataSource dataSource;

    @MockBean
    private OrderMapper orderMapper;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("delete from order_info");
        jdbc.update("delete from appointments_info");
        jdbc.update("delete from appointment_idempotent");
        jdbc.update("delete from user_patient_rel");
        jdbc.update("delete from patients_info");
        jdbc.update("delete from duty_info");
        jdbc.update("insert into duty_info (schedule_id, doctor_id, duty_date, appointments_num, appointments_num_made, registration_fee) values (1, 10, ?, 5, 0, 100.00)",
                Date.valueOf(LocalDate.now().plusDays(1)));
        jdbc.update("insert into patients_info (patient_id, nick_name) values (200, ?)", "测试患者");
        jdbc.update("insert into user_patient_rel (user_id, patient_id) values (100, 200)");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(100L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 订单写入失败_号源与预约全部回滚() {
        when(orderMapper.insertOrder(any(Order.class))).thenReturn(0);

        assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request(1L, "req-order-fail-0001")));

        assertEquals(0L, madeCount(1L));
        assertEquals(0, count("select count(*) from appointments_info"));
        assertEquals(0, count("select count(*) from order_info"));
        // 订单写入失败必须连同幂等记录一并回滚，否则该 requestId 会被伪「处理中」记录永久阻断重试
        assertEquals(0, count("select count(*) from appointment_idempotent"));
    }

    @Test
    void 并发竞争不超卖() throws Exception {
        when(orderMapper.insertOrder(any(Order.class))).thenReturn(1);
        jdbc.update("update duty_info set appointments_num = 1 where schedule_id = 1");

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final String requestId = "req-concurrency-" + i + "-0000";
            futures.add(pool.submit(() -> {
                LoginUser loginUser = new LoginUser();
                loginUser.setUserId(100L);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null));
                try {
                    start.await();
                    orderService.createRegisteredOrder(request(1L, requestId));
                    return true;
                } catch (Exception e) {
                    return false;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }
        start.countDown();
        int success = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                success++;
            }
        }
        pool.shutdown();

        long made = madeCount(1L);
        assertTrue(made <= 1L, "已占用号源不得超过总号源");
        assertEquals(success, made, "成功次数应与排班占用数一致");
        assertEquals(success, count("select count(*) from appointments_info"));
        assertEquals(success, count("select count(*) from appointment_idempotent"),
                "每个成功请求各留下一条幂等记录，失败请求不得留下记录");
    }

    private RegisteredOrderRequest request(Long scheduleId, String requestId) {
        RegisteredOrderRequest request = new RegisteredOrderRequest();
        request.setScheduleId(scheduleId);
        request.setPatientId(200L);
        request.setRequestId(requestId);
        return request;
    }

    private long madeCount(Long scheduleId) {
        Long value = jdbc.queryForObject("select appointments_num_made from duty_info where schedule_id = ?", Long.class, scheduleId);
        return value == null ? 0L : value;
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
