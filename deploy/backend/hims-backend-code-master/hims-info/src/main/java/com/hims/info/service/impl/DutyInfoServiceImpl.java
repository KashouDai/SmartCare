package com.hims.info.service.impl;

import java.util.List;

import com.hims.info.domain.DutyInfo;
import com.hims.info.mapper.DutyInfoMapper;
import com.hims.info.service.IDutyInfoService;
import com.hims.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 排班信息Service业务层处理
 */
@Service
public class DutyInfoServiceImpl implements IDutyInfoService
{
    @Autowired
    private DutyInfoMapper dutyInfoMapper;

    /**
     * 查询排班信息
     *
     * @param scheduleId 排班信息主键
     * @return 排班信息
     */
    @Override
    public DutyInfo selectDutyInfoByScheduleId(Long scheduleId)
    {
        return dutyInfoMapper.selectDutyInfoByScheduleId(scheduleId);
    }

    /**
     * 查询排班信息列表
     *
     * @param dutyInfo 排班信息
     * @return 排班信息
     */
    @Override
    public List<DutyInfo> selectDutyInfoList(DutyInfo dutyInfo)
    {
        return dutyInfoMapper.selectDutyInfoList(dutyInfo);
    }

    /**
     * 新增排班信息
     *
     * @param dutyInfo 排班信息
     * @return 结果
     */
    @Override
    public int insertDutyInfo(DutyInfo dutyInfo)
    {
        dutyInfo.setCreateTime(DateUtils.getNowDate());
        return dutyInfoMapper.insertDutyInfo(dutyInfo);
    }

    /**
     * 修改排班信息
     *
     * @param dutyInfo 排班信息
     * @return 结果
     */
    @Override
    public int updateDutyInfo(DutyInfo dutyInfo)
    {
        dutyInfo.setUpdateTime(DateUtils.getNowDate());
        return dutyInfoMapper.updateDutyInfo(dutyInfo);
    }

    /**
     * 批量删除排班信息
     *
     * @param scheduleIds 需要删除的排班信息主键
     * @return 结果
     */
    @Override
    public int deleteDutyInfoByScheduleIds(Long[] scheduleIds)
    {
        return dutyInfoMapper.deleteDutyInfoByScheduleIdss(scheduleIds);
    }



    /**
     * 删除排班信息信息
     *
     * @param scheduleId 排班信息主键
     * @return 结果
     */
    @Override
    public int deleteDutyInfoByScheduleId(Long scheduleId)
    {
        return dutyInfoMapper.deleteDutyInfoByScheduleId(scheduleId);
    }

    /**
     * 获取值班医生信息
     */
    @Override
    public List<DutyInfo> selectDutyDoctorInfo(DutyInfo dutyInfo) {
        return dutyInfoMapper.selectDutyInfoList(dutyInfo);
    }


}
