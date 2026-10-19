package com.hims.info.service;

import java.util.List;

import com.hims.info.domain.AppointmentsInfo;
import com.hims.info.domain.BookingSchedule;
import com.hims.info.domain.BookingScheduleBo;

/**
 * 预约信息Service接口
 */
public interface IAppointmentsInfoService
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
    public List<AppointmentsInfo> selectAppointmentsInfoList(AppointmentsInfo appointmentsInfo);

    /**
     * 新增预约信息
     *
     * @param appointmentsInfo 预约信息
     * @return 结果
     */
    public String insertAppointmentsInfo(AppointmentsInfo appointmentsInfo);

    /**
     * 修改预约信息
     *
     * @param appointmentsInfo 预约信息
     * @return 结果
     */
    public int updateAppointmentsInfo(AppointmentsInfo appointmentsInfo);

    /**
     * 批量删除预约信息
     *
     * @param appointmentsIds 需要删除的预约信息主键集合
     * @return 结果
     */
    public int deleteAppointmentsInfoByAppointmentsIds(Long[] appointmentsIds);

    /**
     * 删除预约信息信息
     *
     * @param appointmentsId 预约信息主键
     * @return 结果
     */
    public int deleteAppointmentsInfoByAppointmentsId(Long appointmentsId);

    /**
     * 获取预订时间表列表
     */
    List<BookingSchedule> getBookingScheduleList(BookingScheduleBo bo);

    void updateStateById(String outTradeNo, String orderStatePaid);

}
