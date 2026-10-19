package com.hims.order.service.impl;

import com.hims.common.core.domain.model.LoginUser;
import com.hims.common.exception.ServiceException;
import com.hims.info.domain.AppointmentsInfo;
import com.hims.info.domain.DutyInfo;
import com.hims.info.domain.PatientsInfo;
import com.hims.info.mapper.AppointmentsInfoMapper;
import com.hims.info.mapper.DutyInfoMapper;
import com.hims.info.mapper.PatientsInfoMapper;
import com.hims.info.mapper.UserPatientRelationMapper;
import com.hims.order.domain.Order;
import com.hims.order.domain.RegisteredOrderRequest;
import com.hims.order.domain.RegisteredOrderResult;
import com.hims.order.mapper.AppointmentIdempotentMapper;
import com.hims.order.mapper.OrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 统一下单 Service 的参数、归属、排班与写入结果校验。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final Long USER_ID = 100L;
    private static final Long PATIENT_ID = 200L;
    private static final Long SCHEDULE_ID = 300L;
    private static final Long APPOINTMENT_ID = 500L;

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private AppointmentIdempotentMapper appointmentIdempotentMapper;
    @Mock
    private AppointmentsInfoMapper appointmentsInfoMapper;
    @Mock
    private DutyInfoMapper dutyInfoMapper;
    @Mock
    private PatientsInfoMapper patientsInfoMapper;
    @Mock
    private UserPatientRelationMapper userPatientRelationMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void login() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(USER_ID);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRegisteredOrder_正常下单_金额与用户取自服务端() {
        when(patientsInfoMapper.selectPatientsInfoByPatientId(PATIENT_ID)).thenReturn(activePatient());
        when(userPatientRelationMapper.existsRelation(USER_ID, PATIENT_ID)).thenReturn(true);
        when(dutyInfoMapper.selectDutyInfoByScheduleId(SCHEDULE_ID)).thenReturn(availableDuty());
        when(dutyInfoMapper.increaseAppointmentsNumMade(SCHEDULE_ID)).thenReturn(1);
        when(appointmentsInfoMapper.insertAppointmentsInfo(any())).thenReturn(1);
        when(orderMapper.insertOrder(any())).thenReturn(1);

        RegisteredOrderResult result = orderService.createRegisteredOrder(request("8fd8a944-16b9-4f06-b1cf-986a3aa2ac66"));

        assertEquals(new BigDecimal("100.00"), result.getOrderAmount());
        assertEquals("0", result.getPayState());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insertOrder(captor.capture());
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals(new BigDecimal("100.00"), captor.getValue().getOrderAmount());
    }

    @Test
    void createRegisteredOrder_参数非法() {
        assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request("short")));
        verifyNoInteractions(patientsInfoMapper, userPatientRelationMapper, dutyInfoMapper, orderMapper);
    }

    @Test
    void createRegisteredOrder_患者不存在() {
        when(patientsInfoMapper.selectPatientsInfoByPatientId(PATIENT_ID)).thenReturn(null);
        assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request("8fd8a944-16b9-4f06-b1cf-986a3aa2ac66")));
        verifyNoInteractions(userPatientRelationMapper);
    }

    @Test
    void createRegisteredOrder_患者已删除() {
        PatientsInfo deleted = activePatient();
        deleted.setDelFlag("2");
        when(patientsInfoMapper.selectPatientsInfoByPatientId(PATIENT_ID)).thenReturn(deleted);
        assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request("8fd8a944-16b9-4f06-b1cf-986a3aa2ac66")));
        verifyNoInteractions(userPatientRelationMapper);
    }

    @Test
    void createRegisteredOrder_患者无归属() {
        when(patientsInfoMapper.selectPatientsInfoByPatientId(PATIENT_ID)).thenReturn(activePatient());
        when(userPatientRelationMapper.existsRelation(USER_ID, PATIENT_ID)).thenReturn(false);
        assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request("8fd8a944-16b9-4f06-b1cf-986a3aa2ac66")));
        verifyNoInteractions(dutyInfoMapper);
    }

    @Test
    void createRegisteredOrder_排班不存在() {
        when(patientsInfoMapper.selectPatientsInfoByPatientId(PATIENT_ID)).thenReturn(activePatient());
        when(userPatientRelationMapper.existsRelation(USER_ID, PATIENT_ID)).thenReturn(true);
        when(dutyInfoMapper.selectDutyInfoByScheduleId(SCHEDULE_ID)).thenReturn(null);
        assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request("8fd8a944-16b9-4f06-b1cf-986a3aa2ac66")));
        verify(dutyInfoMapper, never()).increaseAppointmentsNumMade(anyLong());
    }

    @Test
    void createRegisteredOrder_号源耗尽() {
        when(patientsInfoMapper.selectPatientsInfoByPatientId(PATIENT_ID)).thenReturn(activePatient());
        when(userPatientRelationMapper.existsRelation(USER_ID, PATIENT_ID)).thenReturn(true);
        when(dutyInfoMapper.selectDutyInfoByScheduleId(SCHEDULE_ID)).thenReturn(availableDuty());
        when(dutyInfoMapper.increaseAppointmentsNumMade(SCHEDULE_ID)).thenReturn(0);
        assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request("8fd8a944-16b9-4f06-b1cf-986a3aa2ac66")));
        verify(appointmentsInfoMapper, never()).insertAppointmentsInfo(any());
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    void createRegisteredOrder_预约写入失败() {
        when(patientsInfoMapper.selectPatientsInfoByPatientId(PATIENT_ID)).thenReturn(activePatient());
        when(userPatientRelationMapper.existsRelation(USER_ID, PATIENT_ID)).thenReturn(true);
        when(dutyInfoMapper.selectDutyInfoByScheduleId(SCHEDULE_ID)).thenReturn(availableDuty());
        when(dutyInfoMapper.increaseAppointmentsNumMade(SCHEDULE_ID)).thenReturn(1);
        when(appointmentsInfoMapper.insertAppointmentsInfo(any())).thenReturn(0);
        assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request("8fd8a944-16b9-4f06-b1cf-986a3aa2ac66")));
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    void createRegisteredOrder_订单写入失败() {
        when(patientsInfoMapper.selectPatientsInfoByPatientId(PATIENT_ID)).thenReturn(activePatient());
        when(userPatientRelationMapper.existsRelation(USER_ID, PATIENT_ID)).thenReturn(true);
        when(dutyInfoMapper.selectDutyInfoByScheduleId(SCHEDULE_ID)).thenReturn(availableDuty());
        when(dutyInfoMapper.increaseAppointmentsNumMade(SCHEDULE_ID)).thenReturn(1);
        when(appointmentsInfoMapper.insertAppointmentsInfo(any())).thenReturn(1);
        when(orderMapper.insertOrder(any())).thenReturn(0);
        assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request("8fd8a944-16b9-4f06-b1cf-986a3aa2ac66")));
    }

    /**
     * 预约表上除有效预约唯一索引外还有主键 (appointments_id)。
     * 主键冲突属系统故障，不能伪装成「该患者已预约该排班」。
     * 异常消息取自 H2 1.4.200 实际抛出的主键冲突。
     */
    @Test
    void createRegisteredOrder_预约主键冲突_不误报为已预约() {
        stubUntilAppointmentInsert();
        when(appointmentsInfoMapper.insertAppointmentsInfo(any())).thenThrow(new DuplicateKeyException(
                "Unique index or primary key violation: \"PRIMARY KEY ON PUBLIC.APPOINTMENTS_INFO(APPOINTMENTS_ID)\""));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request("8fd8a944-16b9-4f06-b1cf-986a3aa2ac66")));

        assertFalse(exception.getMessage().contains("已预约"),
                "预约主键冲突不是重复预约，不能误报为已预约：" + exception.getMessage());
        verify(orderMapper, never()).insertOrder(any());
    }

    /**
     * 有效预约唯一索引冲突才是重复预约。
     * 异常消息取自 MySQL 5.7 实际抛出的唯一索引冲突，用于覆盖生产方言（H2 消息格式不同）。
     */
    @Test
    void createRegisteredOrder_有效预约唯一索引冲突_报已预约() {
        stubUntilAppointmentInsert();
        when(appointmentsInfoMapper.insertAppointmentsInfo(any())).thenThrow(new DuplicateKeyException(
                "Duplicate entry '300-200' for key 'uk_active_schedule_patient'"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.createRegisteredOrder(request("8fd8a944-16b9-4f06-b1cf-986a3aa2ac66")));

        assertTrue(exception.getMessage().contains("已预约"), exception.getMessage());
        verify(orderMapper, never()).insertOrder(any());
    }

    /**
     * 管理端新增订单入口：客户端传入的金额与用户身份必须被忽略，改由服务端按预约所属排班推导，
     * 否则任何拿到该权限的账号都能任意改价或把订单挂到他人名下。
     */
    @Test
    void insertOrder_忽略客户端金额与用户_以服务端推导为准() {
        when(appointmentsInfoMapper.selectAppointmentsInfoByAppointmentsId(APPOINTMENT_ID)).thenReturn(appointment());
        when(dutyInfoMapper.selectDutyInfoByScheduleId(SCHEDULE_ID)).thenReturn(availableDuty());
        when(orderMapper.insertOrder(any())).thenReturn(1);

        Order clientOrder = new Order();
        clientOrder.setAppointmentsId(APPOINTMENT_ID);
        clientOrder.setUserId(999L);
        clientOrder.setOrderAmount(new BigDecimal("0.01"));

        String orderId = orderService.insertOrder(clientOrder);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insertOrder(captor.capture());
        assertEquals(USER_ID, captor.getValue().getUserId(), "用户身份必须取自登录态");
        assertEquals(new BigDecimal("100.00"), captor.getValue().getOrderAmount(), "金额必须取自排班费用");
        assertEquals(captor.getValue().getOrderId() + "", orderId);
    }

    @Test
    void insertOrder_缺少预约ID_拒绝() {
        Order clientOrder = new Order();
        clientOrder.setUserId(USER_ID);
        clientOrder.setOrderAmount(new BigDecimal("0.01"));

        assertThrows(ServiceException.class, () -> orderService.insertOrder(clientOrder));
        verifyNoInteractions(appointmentsInfoMapper, dutyInfoMapper);
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    void insertOrder_预约不存在_拒绝() {
        when(appointmentsInfoMapper.selectAppointmentsInfoByAppointmentsId(APPOINTMENT_ID)).thenReturn(null);

        assertThrows(ServiceException.class, () -> orderService.insertOrder(clientOrder()));
        verifyNoInteractions(dutyInfoMapper);
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    void insertOrder_排班不存在_拒绝() {
        when(appointmentsInfoMapper.selectAppointmentsInfoByAppointmentsId(APPOINTMENT_ID)).thenReturn(appointment());
        when(dutyInfoMapper.selectDutyInfoByScheduleId(SCHEDULE_ID)).thenReturn(null);

        assertThrows(ServiceException.class, () -> orderService.insertOrder(clientOrder()));
        verify(orderMapper, never()).insertOrder(any());
    }

    private Order clientOrder() {
        Order order = new Order();
        order.setAppointmentsId(APPOINTMENT_ID);
        order.setOrderAmount(new BigDecimal("0.01"));
        return order;
    }

    private AppointmentsInfo appointment() {
        AppointmentsInfo appointment = new AppointmentsInfo();
        appointment.setAppointmentsId(APPOINTMENT_ID);
        appointment.setScheduleId(SCHEDULE_ID);
        appointment.setPatientId(PATIENT_ID);
        return appointment;
    }

    private void stubUntilAppointmentInsert() {
        when(patientsInfoMapper.selectPatientsInfoByPatientId(PATIENT_ID)).thenReturn(activePatient());
        when(userPatientRelationMapper.existsRelation(USER_ID, PATIENT_ID)).thenReturn(true);
        when(dutyInfoMapper.selectDutyInfoByScheduleId(SCHEDULE_ID)).thenReturn(availableDuty());
        when(dutyInfoMapper.increaseAppointmentsNumMade(SCHEDULE_ID)).thenReturn(1);
    }

    private RegisteredOrderRequest request(String requestId) {
        RegisteredOrderRequest request = new RegisteredOrderRequest();
        request.setScheduleId(SCHEDULE_ID);
        request.setPatientId(PATIENT_ID);
        request.setRequestId(requestId);
        return request;
    }

    private PatientsInfo activePatient() {
        PatientsInfo patient = new PatientsInfo();
        patient.setPatientId(PATIENT_ID);
        patient.setDelFlag("0");
        return patient;
    }

    private DutyInfo availableDuty() {
        DutyInfo duty = new DutyInfo();
        duty.setScheduleId(SCHEDULE_ID);
        duty.setDoctorId(400L);
        duty.setDutyDate(new Date(System.currentTimeMillis() + 86400000L));
        duty.setAppointmentsNum(5L);
        duty.setAppointmentsNumMade(0L);
        duty.setRegistrationFee(new BigDecimal("100.00"));
        duty.setDelFlag("0");
        return duty;
    }
}
