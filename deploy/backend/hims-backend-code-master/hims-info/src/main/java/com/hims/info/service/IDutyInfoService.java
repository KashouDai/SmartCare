package com.hims.info.service;

import java.util.List;

import com.hims.info.domain.DutyInfo;

/**
 * 排班信息Service接口
 */
public interface IDutyInfoService
{
    /**
     * 查询排班信息
     *
     * @param scheduleId 排班信息主键
     * @return 排班信息
     */
    public DutyInfo selectDutyInfoByScheduleId(Long scheduleId);

    /**
     * 查询排班信息列表
     *
     * @param dutyInfo 排班信息
     * @return 排班信息集合
     */
    public List<DutyInfo> selectDutyInfoList(DutyInfo dutyInfo);

    /**
     * 新增排班信息
     *
     * @param dutyInfo 排班信息
     * @return 结果
     */
    public int insertDutyInfo(DutyInfo dutyInfo);

    /**
     * 修改排班信息
     *
     * @param dutyInfo 排班信息
     * @return 结果
     */
    public int updateDutyInfo(DutyInfo dutyInfo);

    /**
     * 批量删除排班信息
     *
     * @param scheduleIds 需要删除的排班信息主键集合
     * @return 结果
     */
    public int deleteDutyInfoByScheduleIds(Long[] scheduleIds);



    /**
     * 删除排班信息信息
     *
     * @param scheduleId 排班信息主键
     * @return 结果
     */
    public int deleteDutyInfoByScheduleId(Long scheduleId);

    /**
     * 获取值班医生信息
     */
    List<DutyInfo> selectDutyDoctorInfo(DutyInfo dutyInfo);


}
