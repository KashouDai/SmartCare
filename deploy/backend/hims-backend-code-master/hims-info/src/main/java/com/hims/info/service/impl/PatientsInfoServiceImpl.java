package com.hims.info.service.impl;

import java.util.List;

import com.hims.info.domain.PatientsInfo;
import com.hims.info.mapper.PatientsInfoMapper;
import com.hims.info.mapper.UserPatientRelationMapper;
import com.hims.info.service.IPatientsInfoService;
import com.hims.common.utils.DateUtils;
import com.hims.common.utils.SecurityUtils;
import com.hims.common.utils.uuid.SnowflakeIdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 患者信息Service业务层处理
 */
@Service
public class PatientsInfoServiceImpl implements IPatientsInfoService
{
    @Autowired
    private PatientsInfoMapper patientsInfoMapper;

    @Autowired
    private UserPatientRelationMapper userPatientRelationMapper;

    /**
     * 查询患者信息
     *
     * @param patientId 患者信息主键
     * @return 患者信息
     */
    @Override
    public PatientsInfo selectPatientsInfoByPatientId(Long patientId)
    {
        return patientsInfoMapper.selectPatientsInfoByPatientId(patientId);
    }

    /**
     * 查询患者信息列表
     *
     * @param patientsInfo 患者信息
     * @return 患者信息
     */
    @Override
    public List<PatientsInfo> selectPatientsInfoList(PatientsInfo patientsInfo)
    {
        return patientsInfoMapper.selectPatientsInfoList(patientsInfo);
    }

    /**
     * 查询登录用户绑定的患者信息列表
     *
     * @param userId 登录用户ID
     * @return 患者信息集合
     */
    @Override
    public List<PatientsInfo> selectPatientsByUserId(Long userId)
    {
        return patientsInfoMapper.selectPatientsByUserId(userId);
    }

    /**
     * 新增患者信息
     *
     * @param patientsInfo 患者信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertPatientsInfo(PatientsInfo patientsInfo)
    {
        Long userId = SecurityUtils.getUserId();
//        patientsInfo.setPatientId(new SnowflakeIdUtils(5,8).nextId());
        patientsInfo.setCardNo(String.valueOf(new SnowflakeIdUtils(5,8).nextId()));
        patientsInfo.setCreateTime(DateUtils.getNowDate());
        int result = patientsInfoMapper.insertPatientsInfo(patientsInfo);
        // 超级管理员经管理端创建的患者不建立归属关系，避免默认绑定到管理员账号
        if (result > 0 && patientsInfo.getPatientId() != null && !SecurityUtils.isAdmin(userId)) {
            userPatientRelationMapper.insertRelation(userId, patientsInfo.getPatientId());
        }
        return result;
    }

    /**
     * 修改患者信息
     *
     * @param patientsInfo 患者信息
     * @return 结果
     */
    @Override
    public int updatePatientsInfo(PatientsInfo patientsInfo)
    {
        patientsInfo.setUpdateTime(DateUtils.getNowDate());
        return patientsInfoMapper.updatePatientsInfo(patientsInfo);
    }

    /**
     * 批量删除患者信息
     *
     * @param patientIds 需要删除的患者信息主键
     * @return 结果
     */
    @Override
    public int deletePatientsInfoByPatientIds(Long[] patientIds)
    {
        return patientsInfoMapper.deletePatientsInfoByPatientIds(patientIds);
    }

    /**
     * 删除患者信息信息
     *
     * @param patientId 患者信息主键
     * @return 结果
     */
    @Override
    public int deletePatientsInfoByPatientId(Long patientId)
    {
        return patientsInfoMapper.deletePatientsInfoByPatientId(patientId);
    }
}
