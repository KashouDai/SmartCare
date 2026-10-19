package com.hims.info.mapper;

import java.util.List;

import com.hims.info.domain.MedicalHistory;

/**
 * 就诊历史Mapper接口
 */
public interface MedicalHistoryMapper
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
     * 删除就诊历史
     *
     * @param historyId 就诊历史主键
     * @return 结果
     */
    public int deleteMedicalHistoryByHistoryId(Long historyId);

    /**
     * 批量删除就诊历史
     *
     * @param historyIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMedicalHistoryByHistoryIds(Long[] historyIds);
}
