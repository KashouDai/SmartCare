package com.hims.info.service;

import java.util.List;

import com.hims.info.domain.MedicalHistory;

/**
 * 就诊历史Service接口
 */
public interface IMedicalHistoryService
{
    /**
     * 查询就诊历史
     *
     * @param historyId 就诊历史主键
     * @return 就诊历史
     */
    public MedicalHistory selectMedicalHistoryByHistoryId(Long historyId);

    /**
     * 查询就诊历史列表
     *
     * @param medicalHistory 就诊历史
     * @return 就诊历史集合
     */
    public List<MedicalHistory> selectMedicalHistoryList(MedicalHistory medicalHistory);

    /**
     * 新增就诊历史
     *
     * @param medicalHistory 就诊历史
     * @return 结果
     */
    public int insertMedicalHistory(MedicalHistory medicalHistory);

    /**
     * 修改就诊历史
     *
     * @param medicalHistory 就诊历史
     * @return 结果
     */
    public int updateMedicalHistory(MedicalHistory medicalHistory);

    /**
     * 批量删除就诊历史
     *
     * @param historyIds 需要删除的就诊历史主键集合
     * @return 结果
     */
    public int deleteMedicalHistoryByHistoryIds(Long[] historyIds);

    /**
     * 删除就诊历史信息
     *
     * @param historyId 就诊历史主键
     * @return 结果
     */
    public int deleteMedicalHistoryByHistoryId(Long historyId);
}
