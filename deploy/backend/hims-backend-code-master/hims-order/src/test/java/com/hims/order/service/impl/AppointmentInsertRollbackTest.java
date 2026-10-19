package com.hims.order.service.impl;

import com.hims.common.core.domain.model.LoginUser;
import com.hims.common.exception.ServiceException;
import com.hims.info.mapper.AppointmentsInfoMapper;
import com.hims.order.domain.RegisteredOrderRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = RegisteredOrderTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AppointmentInsertRollbackTest {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private DataSource dataSource;

    @MockBean
    private AppointmentsInfoMapper appointmentsInfoMapper;

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
    void 预约写入失败_号源回滚() {
        when(appointmentsInfoMapper.insertAppointmentsInfo(any())).thenReturn(0);

        RegisteredOrderRequest request = new RegisteredOrderRequest();
        request.setScheduleId(1L);
        request.setPatientId(200L);
        request.setRequestId("req-appointment-fail-0001");

        assertThrows(ServiceException.class, () -> orderService.createRegisteredOrder(request));

        Long made = jdbc.queryForObject("select appointments_num_made from duty_info where schedule_id = 1", Long.class);
        assertEquals(0L, made == null ? 0L : made);
        assertEquals(0, count("select count(*) from order_info"));
        // 预约写入失败必须连同幂等记录一并回滚，否则该 requestId 会被伪「处理中」记录永久阻断重试
        assertEquals(0, count("select count(*) from appointment_idempotent"));
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
