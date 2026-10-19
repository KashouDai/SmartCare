package com.hims.info.service.impl;

import cn.hutool.core.date.DateUtil;
import com.hims.info.domain.*;
import com.hims.info.mapper.AppointmentsInfoMapper;
import com.hims.info.mapper.DutyInfoMapper;
import com.hims.info.mapper.HospitalInfoMapper;
import com.hims.info.service.IAppointmentsInfoService;
import com.hims.common.core.page.PageDomain;
import com.hims.common.core.page.TableSupport;
import com.hims.common.utils.DateUtils;
import com.hims.common.utils.uuid.SnowflakeIdUtils;
import com.hims.info.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预约信息Service业务层处理
 */
@Service
public class AppointmentsInfoServiceImpl implements IAppointmentsInfoService {
    @Autowired
    private AppointmentsInfoMapper appointmentsInfoMapper;

    @Autowired
    private HospitalInfoMapper hospitalInfoMapper;

    @Autowired
    private DutyInfoMapper dutyInfoMapper;

    /**
     * 查询预约信息
     *
     * @param appointmentsId 预约信息主键
     * @return 预约信息
     */
    @Override
    public AppointmentsInfo selectAppointmentsInfoByAppointmentsId(Long appointmentsId) {
        return appointmentsInfoMapper.selectAppointmentsInfoByAppointmentsId(appointmentsId);
    }

    /**
     * 查询预约信息列表
     *
     * @param appointmentsInfo 预约信息
     * @return 预约信息
     */
    @Override
    public List<AppointmentsInfo> selectAppointmentsInfoList(AppointmentsInfo appointmentsInfo)
    {
        return appointmentsInfoMapper.selectAppointmentsInfoList(appointmentsInfo);
    }

    /**
     * 新增预约信息
     *
     * @param appointmentsInfo 预约信息
     * @return 结果
     */
    @Override
    public String insertAppointmentsInfo(AppointmentsInfo appointmentsInfo) {
        appointmentsInfo.setCreateTime(DateUtils.getNowDate());
        appointmentsInfo.setAppointmentsId(new SnowflakeIdUtils(5, 8).nextId());
        appointmentsInfoMapper.insertAppointmentsInfo(appointmentsInfo);

        // 排班信息已预约数加一
        DutyInfo dutyInfo = dutyInfoMapper.selectDutyInfoByScheduleId(appointmentsInfo.getScheduleId());
        dutyInfo.setAppointmentsNumMade(dutyInfo.getAppointmentsNumMade() + 1);
        dutyInfoMapper.updateDutyInfo(dutyInfo);
        return appointmentsInfo.getAppointmentsId() + "";
    }

    /**
     * 修改预约信息
     *
     * @param appointmentsInfo 预约信息
     * @return 结果
     */
    @Override
    public int updateAppointmentsInfo(AppointmentsInfo appointmentsInfo)
    {
        appointmentsInfo.setUpdateTime(DateUtils.getNowDate());
        return appointmentsInfoMapper.updateAppointmentsInfo(appointmentsInfo);
    }

    /**
     * 批量删除预约信息
     *
     * @param appointmentsIds 需要删除的预约信息主键
     * @return 结果
     */
    @Override
    public int deleteAppointmentsInfoByAppointmentsIds(Long[] appointmentsIds)
    {
        return appointmentsInfoMapper.deleteAppointmentsInfoByAppointmentsIds(appointmentsIds);
    }

    /**
     * 删除预约信息信息
     *
     * @param appointmentsId 预约信息主键
     * @return 结果
     */
    @Override
    public int deleteAppointmentsInfoByAppointmentsId(Long appointmentsId) {
        return appointmentsInfoMapper.deleteAppointmentsInfoByAppointmentsId(appointmentsId);
    }

    /**
     * 获取预订时间表列表
     */
    @Override
    public List<BookingSchedule> getBookingScheduleList(BookingScheduleBo bo) {
        // 获取分页信息
        PageDomain pageDomain = TableSupport.getPageDomain();
        int pageNum = pageDomain.getPageNum();
        int pageSize = pageDomain.getPageSize();

        // 查询医院信息
        HospitalInfo hospitalInfo = hospitalInfoMapper.selectHospitalInfoByCode(bo.getHospitalCode());
        int appointmentDay = hospitalInfo.getAppointmentDay();

        // 计算起始日期和数量
        int offset = (pageNum - 1) * pageSize;
        Date startDate = offset > 0 ? DateUtil.offsetDay(DateUtils.getNowDate(), offset) : DateUtils.getNowDate();
        int daysToAdd = Math.min(pageSize, appointmentDay - offset);

        // 生成日期列表
        List<Date> dateList = DateUtils.calculateDates(startDate, daysToAdd);
        if(dateList.size() > pageSize) {
            dateList.remove(pageSize);
        }

        // 构建预约排班信息
        return dateList.stream().map(date -> {
            BookingSchedule schedule = new BookingSchedule();
            String formattedDate = DateUtil.formatDate(date);
            schedule.setDate(formattedDate);
            schedule.setWeekday(DateUtil.dayOfWeekEnum(date).toChinese());
            schedule.setState(DateUtils.checkTime(date, hospitalInfo.getReleaseTime(), hospitalInfo.getSuspendTime(), appointmentDay));
            // 查询当天是否有号
            schedule.setSurplusState(dutyInfoMapper.selectDutyStateByDate(formattedDate, bo.getHospitalCode(), bo.getDeptId()));
            return schedule;
        }).collect(Collectors.toList());
    }

    public void updateStateById(String outTradeNo, String orderStatePaid) {
        appointmentsInfoMapper.updateStateById(outTradeNo, orderStatePaid);
    }
}
