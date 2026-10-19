package com.hims.order.service;

import com.hims.common.constant.Constants;
import com.hims.common.exception.ServiceException;
import com.hims.info.domain.AppointmentsInfo;
import com.hims.info.mapper.AppointmentsInfoMapper;
import com.hims.info.mapper.DutyInfoMapper;
import com.hims.order.domain.Order;
import com.hims.order.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 订单支付生命周期：所有状态变化和号源释放必须在同一事务内完成。
 */
@Service
public class OrderLifecycleService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private AppointmentsInfoMapper appointmentsInfoMapper;

    @Resource
    private DutyInfoMapper dutyInfoMapper;

    @Transactional(rollbackFor = Exception.class)
    public boolean markPaid(Long orderId) {
        Order order = requireOrder(orderId);
        if (orderMapper.updatePayStateIfCurrent(orderId, Constants.ORDER_STATE_PENDING_PAYMENT,
                Constants.ORDER_STATE_PAID) != 1) {
            return Constants.ORDER_STATE_PAID.equals(order.getPayState());
        }
        updateAppointment(order, Constants.ORDER_STATE_PENDING_PAYMENT, Constants.ORDER_STATE_PAID, false);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelPending(Long orderId) {
        Order order = requireOrder(orderId);
        if (orderMapper.updatePayStateIfCurrent(orderId, Constants.ORDER_STATE_PENDING_PAYMENT,
                Constants.ORDER_STATE_CANCELED) != 1) {
            return Constants.ORDER_STATE_CANCELED.equals(order.getPayState());
        }
        updateAppointment(order, Constants.ORDER_STATE_PENDING_PAYMENT, Constants.ORDER_STATE_CANCELED, true);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean refundPaid(Long orderId) {
        Order order = requireOrder(orderId);
        if (orderMapper.updatePayStateIfCurrent(orderId, Constants.ORDER_STATE_PAID,
                Constants.ORDER_STATE_REFUNDED) != 1) {
            return Constants.ORDER_STATE_REFUNDED.equals(order.getPayState());
        }
        updateAppointment(order, Constants.ORDER_STATE_PAID, Constants.ORDER_STATE_REFUNDED, true);
        return true;
    }

    /** 每分钟扫描一次过期待支付订单；条件更新保证重复扫描不会重复释放号源。 */
    @Scheduled(fixedDelayString = "${order.payment-expiry.scan-ms:60000}")
    @Transactional(rollbackFor = Exception.class)
    public void expirePendingOrders() {
        List<Order> expired = orderMapper.selectExpiredPendingOrders(new Date());
        for (Order order : expired) {
            if (orderMapper.updatePayStateIfCurrent(order.getOrderId(), Constants.ORDER_STATE_PENDING_PAYMENT,
                    Constants.ORDER_STATE_CANCELED) == 1) {
                updateAppointment(order, Constants.ORDER_STATE_PENDING_PAYMENT,
                        Constants.ORDER_STATE_CANCELED, true);
            }
        }
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectOrderByOrderId(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        return order;
    }

    private void updateAppointment(Order order, String fromState, String toState, boolean releaseSlot) {
        AppointmentsInfo appointment = appointmentsInfoMapper
                .selectAppointmentsInfoByAppointmentsId(order.getAppointmentsId());
        if (appointment == null || appointmentsInfoMapper.updateStateIfCurrent(
                appointment.getAppointmentsId(), fromState, toState) != 1) {
            throw new ServiceException("预约状态已变化，请稍后查询");
        }
        if (releaseSlot && dutyInfoMapper.decreaseAppointmentsNumMade(appointment.getScheduleId()) != 1) {
            throw new ServiceException("号源释放失败");
        }
    }
}
