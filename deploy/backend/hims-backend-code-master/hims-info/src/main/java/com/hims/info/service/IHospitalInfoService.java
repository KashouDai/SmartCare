package com.hims.info.service;

import java.util.List;

import com.hims.info.domain.HospitalInfo;

/**
 * 医院管理Service接口
 */
public interface IHospitalInfoService
{
    /**
     * 查询医院管理
     *
     * @param id 医院管理主键
     * @return 医院管理
     */
    public HospitalInfo selectHospitalInfoById(Long id);

    /**
     * 查询医院管理列表
     *
     * @param hospitalInfo 医院管理
     * @return 医院管理集合
     */
    public List<HospitalInfo> selectHospitalInfoList(HospitalInfo hospitalInfo);

    /**
     * 新增医院管理
     *
     * @param hospitalInfo 医院管理
     * @return 结果
     */
    public int insertHospitalInfo(HospitalInfo hospitalInfo);

    /**
     * 修改医院管理
     *
     * @param hospitalInfo 医院管理
     * @return 结果
     */
    public int updateHospitalInfo(HospitalInfo hospitalInfo);

    /**
     * 批量删除医院管理
     *
     * @param ids 需要删除的医院管理主键集合
     * @return 结果
     */
    public int deleteHospitalInfoByIds(Long[] ids);

    /**
     * 删除医院管理信息
     *
     * @param id 医院管理主键
     * @return 结果
     */
    public int deleteHospitalInfoById(Long id);

    List<HospitalInfo> getList();

    /**
     * 按Code获取医院详细信息
     */
    HospitalInfo selectHospitalInfoByCode(String hoscode);
}
