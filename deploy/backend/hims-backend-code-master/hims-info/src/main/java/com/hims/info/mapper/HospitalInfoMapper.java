package com.hims.info.mapper;

import java.util.List;
import com.hims.info.domain.HospitalInfo;

/**
 * 医院信息Mapper接口
 */
public interface HospitalInfoMapper
{
    /**
     * 查询医院信息

     *
     * @param id 医院信息
    主键
     * @return 医院信息

     */
    public HospitalInfo selectHospitalInfoById(Long id);

    /**
     * 查询医院信息
     列表
     *
     * @param hospitalInfo 医院信息

     * @return 医院信息
    集合
     */
    public List<HospitalInfo> selectHospitalInfoList(HospitalInfo hospitalInfo);

    /**
     * 新增医院信息

     *
     * @param hospitalInfo 医院信息

     * @return 结果
     */
    public int insertHospitalInfo(HospitalInfo hospitalInfo);

    /**
     * 修改医院信息

     *
     * @param hospitalInfo 医院信息

     * @return 结果
     */
    public int updateHospitalInfo(HospitalInfo hospitalInfo);

    /**
     * 删除医院信息

     *
     * @param id 医院信息
    主键
     * @return 结果
     */
    public int deleteHospitalInfoById(Long id);

    /**
     * 批量删除医院信息

     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHospitalInfoByIds(Long[] ids);

    List<HospitalInfo> selectList();

    HospitalInfo selectHospitalInfoByCode(String hoscode);
}
