package com.hims.order.mapper;

import java.util.List;
import com.hims.order.domain.PayRecord;

/**
 * 支付信息Mapper接口
 */
public interface PayRecordMapper
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
     * 删除支付信息
     *
     * @param tradeNo 支付信息主键
     * @return 结果
     */
    public int deletePayRecordByTradeNo(String tradeNo);

    /**
     * 批量删除支付信息
     *
     * @param tradeNos 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePayRecordByTradeNos(String[] tradeNos);

    PayRecord selectPayRecordByOutTradeNo(String outTradeNo);
}
