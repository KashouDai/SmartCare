package com.hims.order.service.impl;

import java.util.List;

import com.hims.order.domain.PayRecord;
import com.hims.order.mapper.PayRecordMapper;
import com.hims.order.service.IPayRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 支付信息Service业务层处理
 */
@Service
public class PayRecordServiceImpl implements IPayRecordService
{
    @Autowired
    private PayRecordMapper payRecordMapper;

    /**
     * 查询支付信息
     *
     * @param tradeNo 支付信息主键
     * @return 支付信息
     */
    @Override
    public PayRecord selectPayRecordByTradeNo(String tradeNo)
    {
        return payRecordMapper.selectPayRecordByTradeNo(tradeNo);
    }

    /**
     * 查询支付信息列表
     *
     * @param payRecord 支付信息
     * @return 支付信息
     */
    @Override
    public List<PayRecord> selectPayRecordList(PayRecord payRecord)
    {
        return payRecordMapper.selectPayRecordList(payRecord);
    }

    /**
     * 新增支付信息
     *
     * @param payRecord 支付信息
     * @return 结果
     */
    @Override
    public int insertPayRecord(PayRecord payRecord)
    {
        return payRecordMapper.insertPayRecord(payRecord);
    }

    /**
     * 修改支付信息
     *
     * @param payRecord 支付信息
     * @return 结果
     */
    @Override
    public int updatePayRecord(PayRecord payRecord)
    {
        return payRecordMapper.updatePayRecord(payRecord);
    }

    /**
     * 批量删除支付信息
     *
     * @param tradeNos 需要删除的支付信息主键
     * @return 结果
     */
    @Override
    public int deletePayRecordByTradeNos(String[] tradeNos)
    {
        return payRecordMapper.deletePayRecordByTradeNos(tradeNos);
    }

    /**
     * 删除支付信息信息
     *
     * @param tradeNo 支付信息主键
     * @return 结果
     */
    @Override
    public int deletePayRecordByTradeNo(String tradeNo)
    {
        return payRecordMapper.deletePayRecordByTradeNo(tradeNo);
    }

    @Override
    public PayRecord selectPayRecordByOutTradeNo(String outTradeNo) {

        return payRecordMapper.selectPayRecordByOutTradeNo(outTradeNo);
    }
}
