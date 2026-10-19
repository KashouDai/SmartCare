package com.hims.info.mapper;

import java.util.List;

import com.hims.info.domain.AppointmentsInfo;
import org.apache.ibatis.annotations.Param;

/**
 * 预约信息Mapper接口
 */
public interface AppointmentsInfoMapper
{
    /**
     * 查询预约信息
     *
     * @param appointmentsId 预约信息主键
     * @return 预约信息
     */
    public AppointmentsInfo selectAppointmentsInfoByAppointmentsId(Long appointmentsId);

    /**
     * 查询预约信息列表
     *
     * @param appointmentsInfo 预约信息
     * @return 预约信息集合
     */
    public List<AppointmentsInfo> selectAppointmentsInfoList(@Param("data") AppointmentsInfo appointmentsInfo);

    /**
     * 统计某患者在某排班下的有效预约数量
     *
     * @param scheduleId 排班ID
     * @param patientId 患者ID
     * @return 有效预约数量
     */
    public int countActiveByScheduleAndPatient(@Param("scheduleId") Long scheduleId, @Param("patientId") Long patientId);

    /**
     * 新增预约信息
     *
     * @param appointmentsInfo 预约信息
     * @return 结果
     */
    public int insertAppointmentsInfo(AppointmentsInfo appointmentsInfo);

    /**
     * 修改预约信息
     *
     * @param appointmentsInfo 预约信息
     * @return 结果
     */
    public int updateAppointmentsInfo(AppointmentsInfo appointmentsInfo);

    /**
     * 删除预约信息
     *
     * @param appointmentsId 预约信息主键
     * @return 结果
     */
    public int deleteAppointmentsInfoByAppointmentsId(Long appointmentsId);

    /**
     * 批量删除预约信息
     *
     * @param appointmentsIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppointmentsInfoByAppointmentsIds(Long[] appointmentsIds);


    void updateStateById(@Param("orderId") String outTradeNo, @Param("state") String orderStatePaid);

    int updateStateIfCurrent(@Param("appointmentsId") Long appointmentsId, @Param("fromState") String fromState,
                             @Param("toState") String toState);
}
