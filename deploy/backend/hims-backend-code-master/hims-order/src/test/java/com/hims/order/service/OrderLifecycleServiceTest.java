package com.hims.order.service;

import com.hims.common.constant.Constants;
import com.hims.info.domain.AppointmentsInfo;
import com.hims.info.mapper.AppointmentsInfoMapper;
import com.hims.info.mapper.DutyInfoMapper;
import com.hims.order.domain.Order;
import com.hims.order.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private AppointmentsInfoMapper appointmentsInfoMapper;

    @Mock
    private DutyInfoMapper dutyInfoMapper;

    @InjectMocks
    private OrderLifecycleService lifecycleService;

    @Test
    void cancelPendingChangesStatesAndReleasesOneSlot() {
        Order order = order(Constants.ORDER_STATE_PENDING_PAYMENT);
        AppointmentsInfo appointment = appointment();
        when(orderMapper.selectOrderByOrderId(1L)).thenReturn(order);
        when(orderMapper.updatePayStateIfCurrent(1L, Constants.ORDER_STATE_PENDING_PAYMENT,
                Constants.ORDER_STATE_CANCELED)).thenReturn(1);
        when(appointmentsInfoMapper.selectAppointmentsInfoByAppointmentsId(10L)).thenReturn(appointment);
        when(appointmentsInfoMapper.updateStateIfCurrent(10L, Constants.ORDER_STATE_PENDING_PAYMENT,
                Constants.ORDER_STATE_CANCELED)).thenReturn(1);
        when(dutyInfoMapper.decreaseAppointmentsNumMade(20L)).thenReturn(1);

        assertTrue(lifecycleService.cancelPending(1L));

        verify(dutyInfoMapper).decreaseAppointmentsNumMade(20L);
    }

    @Test
    void repeatedCancelDoesNotReleaseAgain() {
        when(orderMapper.selectOrderByOrderId(1L)).thenReturn(order(Constants.ORDER_STATE_CANCELED));

        assertTrue(lifecycleService.cancelPending(1L));

        verify(orderMapper).selectOrderByOrderId(1L);
    }

    @Test
    void markPaidUsesPendingStateGuard() {
        Order order = order(Constants.ORDER_STATE_PENDING_PAYMENT);
        AppointmentsInfo appointment = appointment();
        when(orderMapper.selectOrderByOrderId(1L)).thenReturn(order);
        when(orderMapper.updatePayStateIfCurrent(1L, Constants.ORDER_STATE_PENDING_PAYMENT,
                Constants.ORDER_STATE_PAID)).thenReturn(1);
        when(appointmentsInfoMapper.selectAppointmentsInfoByAppointmentsId(10L)).thenReturn(appointment);
        when(appointmentsInfoMapper.updateStateIfCurrent(10L, Constants.ORDER_STATE_PENDING_PAYMENT,
                Constants.ORDER_STATE_PAID)).thenReturn(1);

        assertTrue(lifecycleService.markPaid(1L));

        verify(orderMapper).updatePayStateIfCurrent(eq(1L), eq(Constants.ORDER_STATE_PENDING_PAYMENT),
                eq(Constants.ORDER_STATE_PAID));
    }

    @Test
    void expiryScanReleasesOnlyOrdersWonByConditionalUpdate() {
        Order order = order(Constants.ORDER_STATE_PENDING_PAYMENT);
        AppointmentsInfo appointment = appointment();
        when(orderMapper.selectExpiredPendingOrders(org.mockito.ArgumentMatchers.any())).thenReturn(
                Collections.singletonList(order));
        when(orderMapper.updatePayStateIfCurrent(1L, Constants.ORDER_STATE_PENDING_PAYMENT,
                Constants.ORDER_STATE_CANCELED)).thenReturn(1);
        when(appointmentsInfoMapper.selectAppointmentsInfoByAppointmentsId(10L)).thenReturn(appointment);
        when(appointmentsInfoMapper.updateStateIfCurrent(10L, Constants.ORDER_STATE_PENDING_PAYMENT,
                Constants.ORDER_STATE_CANCELED)).thenReturn(1);
        when(dutyInfoMapper.decreaseAppointmentsNumMade(20L)).thenReturn(1);

        lifecycleService.expirePendingOrders();

        verify(dutyInfoMapper).decreaseAppointmentsNumMade(20L);
    }

    private Order order(String state) {
        Order order = new Order();
        order.setOrderId(1L);
        order.setAppointmentsId(10L);
        order.setPayState(state);
        return order;
    }

    private AppointmentsInfo appointment() {
        AppointmentsInfo appointment = new AppointmentsInfo();
        appointment.setAppointmentsId(10L);
        appointment.setScheduleId(20L);
        return appointment;
    }
}
