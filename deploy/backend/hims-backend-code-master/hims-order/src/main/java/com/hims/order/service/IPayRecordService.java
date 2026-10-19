package com.hims.order.service;

import java.util.List;
import com.hims.order.domain.PayRecord;

/**
 * 支付信息Service接口
 */
public interface IPayRecordService
{
    /**
     * 查询支付信息
     *
     * @param tradeNo 支付信息主键
     * @return 支付信息
     */
    public PayRecord selectPayRecordByTradeNo(String tradeNo);

    /**
     * 查询支付信息列表
     *
     * @param payRecord 支付信息
     * @return 支付信息集合
     */
    public List<PayRecord> selectPayRecordList(PayRecord payRecord);

    /**
     * 新增支付信息
     *
     * @param payRecord 支付信息
     * @return 结果
     */
    public int insertPayRecord(PayRecord payRecord);

    /**
     * 修改支付信息
     *
     * @param payRecord 支付信息
     * @return 结果
     */
    public int updatePayRecord(PayRecord payRecord);

    /**
     * 批量删除支付信息
     *
     * @param tradeNos 需要删除的支付信息主键集合
     * @return 结果
     */
    public int deletePayRecordByTradeNos(String[] tradeNos);

    /**
     * 删除支付信息信息
     *
     * @param tradeNo 支付信息主键
     * @return 结果
     */
    public int deletePayRecordByTradeNo(String tradeNo);

    PayRecord selectPayRecordByOutTradeNo(String outTradeNo);
}
