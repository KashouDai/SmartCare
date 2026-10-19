package com.hims.info.mapper;

import com.hims.info.domain.DutyInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 排班信息Mapper接口
 */
public interface DutyInfoMapper {
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
     * 删除排班信息
     *
     * @param scheduleId 排班信息主键
     * @return 结果
     */
    public int deleteDutyInfoByScheduleId(Long scheduleId);


    /**
     * 批量删除排班信息
     *
     * @param scheduleIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDutyInfoByScheduleIds(Long[] scheduleIds);

    public int deleteDutyInfoByScheduleIdss(Long[] scheduleIds);

    String selectDutyStateByDate(@Param("date") String date, @Param("hospitalCode") String hospitalCode, @Param("deptId") String deptId);

    int increaseAppointmentsNumMade(@Param("scheduleId") Long scheduleId);

    int decreaseAppointmentsNumMade(@Param("scheduleId") Long scheduleId);

}
