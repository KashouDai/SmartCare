package com.hims.info.service.impl;

import java.util.List;

import com.hims.info.domain.MedicalHistory;
import com.hims.info.mapper.MedicalHistoryMapper;
import com.hims.info.service.IMedicalHistoryService;
import com.hims.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 就诊历史Service业务层处理
 */
@Service
public class MedicalHistoryServiceImpl implements IMedicalHistoryService
{
    @Autowired
    private MedicalHistoryMapper medicalHistoryMapper;

    /**
     * 查询就诊历史
     *
     * @param historyId 就诊历史主键
     * @return 就诊历史
     */
    @Override
    public MedicalHistory selectMedicalHistoryByHistoryId(Long historyId)
    {
        return medicalHistoryMapper.selectMedicalHistoryByHistoryId(historyId);
    }

    /**
     * 查询就诊历史列表
     *
     * @param medicalHistory 就诊历史
     * @return 就诊历史
     */
    @Override
    public List<MedicalHistory> selectMedicalHistoryList(MedicalHistory medicalHistory)
    {
        return medicalHistoryMapper.selectMedicalHistoryList(medicalHistory);
    }

    /**
     * 新增就诊历史
     *
     * @param medicalHistory 就诊历史
     * @return 结果
     */
    @Override
    public int insertMedicalHistory(MedicalHistory medicalHistory)
    {
        medicalHistory.setCreateTime(DateUtils.getNowDate());
        return medicalHistoryMapper.insertMedicalHistory(medicalHistory);
    }

    /**
     * 修改就诊历史
     *
     * @param medicalHistory 就诊历史
     * @return 结果
     */
    @Override
    public int updateMedicalHistory(MedicalHistory medicalHistory)
    {
        medicalHistory.setUpdateTime(DateUtils.getNowDate());
        return medicalHistoryMapper.updateMedicalHistory(medicalHistory);
    }

    /**
     * 批量删除就诊历史
     *
     * @param historyIds 需要删除的就诊历史主键
     * @return 结果
     */
    @Override
    public int deleteMedicalHistoryByHistoryIds(Long[] historyIds)
    {
        return medicalHistoryMapper.deleteMedicalHistoryByHistoryIds(historyIds);
    }

    /**
     * 删除就诊历史信息
     *
     * @param historyId 就诊历史主键
     * @return 结果
     */
    @Override
    public int deleteMedicalHistoryByHistoryId(Long historyId)
    {
        return medicalHistoryMapper.deleteMedicalHistoryByHistoryId(historyId);
    }
}
