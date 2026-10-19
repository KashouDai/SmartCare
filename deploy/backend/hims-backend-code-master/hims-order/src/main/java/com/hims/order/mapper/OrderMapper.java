package com.hims.order.mapper;

import java.util.List;
import java.util.Date;
import com.hims.order.domain.Order;
import com.hims.order.domain.RegisteredOrder;
import org.apache.ibatis.annotations.Param;

/**
 * 订单Mapper接口
 */
public interface OrderMapper
{
    /**
     * 查询订单
     *
     * @param orderId 订单主键
     * @return 订单
     */
    public Order selectOrderByOrderId(Long orderId);

    /**
     * 查询订单列表
     *
     * @param order 订单
     * @return 订单集合
     */
    public List<Order> selectOrderList(Order order);

    /**
     * 新增订单
     *
     * @param order 订单
     * @return 结果
     */
    public int insertOrder(Order order);

    /**
     * 修改订单
     *
     * @param order 订单
     * @return 结果
     */
    public int updateOrder(Order order);

    /**
     * 删除订单
     *
     * @param orderId 订单主键
     * @return 结果
     */
    public int deleteOrderByOrderId(Long orderId);

    /**
     * 批量删除订单
     *
     * @param orderIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderByOrderIds(Long[] orderIds);

    List<RegisteredOrder> selectRegisteredOrderListByUserId(@Param("data") Order order);

    void updateOrderStateById(@Param("orderId") String outTradeNo, @Param("state") String orderStatePaid);

    int updatePayStateIfCurrent(@Param("orderId") Long orderId, @Param("fromState") String fromState,
                                @Param("toState") String toState);

    List<Order> selectExpiredPendingOrders(@Param("now") Date now);

}
