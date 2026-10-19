package com.hims.info.mapper;

import java.util.List;

import com.hims.info.domain.PatientsInfo;

/**
 * 患者信息Mapper接口
 */
public interface PatientsInfoMapper
{
    /**
     * 查询患者信息
     *
     * @param patientId 患者信息主键
     * @return 患者信息
     */
    public PatientsInfo selectPatientsInfoByPatientId(Long patientId);

    /**
     * 查询患者信息列表
     *
     * @param patientsInfo 患者信息
     * @return 患者信息集合
     */
    public List<PatientsInfo> selectPatientsInfoList(PatientsInfo patientsInfo);

    /**
     * 查询登录用户绑定的患者信息列表
     *
     * @param userId 登录用户ID
     * @return 患者信息集合
     */
    public List<PatientsInfo> selectPatientsByUserId(Long userId);

    /**
     * 新增患者信息
     *
     * @param patientsInfo 患者信息
     * @return 结果
     */
    public int insertPatientsInfo(PatientsInfo patientsInfo);

    /**
     * 修改患者信息
     *
     * @param patientsInfo 患者信息
     * @return 结果
     */
    public int updatePatientsInfo(PatientsInfo patientsInfo);

    /**
     * 删除患者信息
     *
     * @param patientId 患者信息主键
     * @return 结果
     */
    public int deletePatientsInfoByPatientId(Long patientId);

    /**
     * 批量删除患者信息
     *
     * @param patientIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePatientsInfoByPatientIds(Long[] patientIds);
}
