package com.hims.info.service.impl;

import java.util.List;

import com.hims.info.domain.HospitalInfo;
import com.hims.info.mapper.HospitalInfoMapper;
import com.hims.info.service.IHospitalInfoService;
import com.hims.common.core.redis.BasicInfoCache;
import com.hims.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 医院管理Service业务层处理
 */
@Service
public class HospitalInfoServiceImpl implements IHospitalInfoService
{
    private static final String CACHE_ID_PREFIX = "hospital:info:id:";
    private static final String CACHE_CODE_PREFIX = "hospital:info:code:";

    @Autowired
    private HospitalInfoMapper hospitalInfoMapper;

    @Autowired
    private BasicInfoCache basicInfoCache;

    /**
     * 查询医院管理
     *
     * @param id 医院管理主键
     * @return 医院管理
     */
    @Override
    public HospitalInfo selectHospitalInfoById(Long id)
    {
        return basicInfoCache.getOrLoad(CACHE_ID_PREFIX + id, HospitalInfo.class,
                () -> hospitalInfoMapper.selectHospitalInfoById(id));
    }

    /**
     * 查询医院管理列表
     *
     * @param hospitalInfo 医院管理
     * @return 医院管理
     */
    @Override
    public List<HospitalInfo> selectHospitalInfoList(HospitalInfo hospitalInfo)
    {
        return hospitalInfoMapper.selectHospitalInfoList(hospitalInfo);
    }

    /**
     * 新增医院管理
     *
     * @param hospitalInfo 医院管理
     * @return 结果
     */
    @Override
    public int insertHospitalInfo(HospitalInfo hospitalInfo)
    {
        hospitalInfo.setCreateTime(DateUtils.getNowDate());
        int result = hospitalInfoMapper.insertHospitalInfo(hospitalInfo);
        if (result > 0)
        {
            basicInfoCache.evictAfterCommit(idKey(hospitalInfo.getId()), codeKey(hospitalInfo.getHospitalCode()));
        }
        return result;
    }

    /**
     * 修改医院管理
     *
     * @param hospitalInfo 医院管理
     * @return 结果
     */
    @Override
    public int updateHospitalInfo(HospitalInfo hospitalInfo)
    {
        HospitalInfo old = hospitalInfoMapper.selectHospitalInfoById(hospitalInfo.getId());
        hospitalInfo.setUpdateTime(DateUtils.getNowDate());
        int result = hospitalInfoMapper.updateHospitalInfo(hospitalInfo);
        if (result > 0)
        {
            basicInfoCache.evictAfterCommit(idKey(hospitalInfo.getId()),
                    old == null ? null : codeKey(old.getHospitalCode()), codeKey(hospitalInfo.getHospitalCode()));
        }
        return result;
    }

    /**
     * 批量删除医院管理
     *
     * @param ids 需要删除的医院管理主键
     * @return 结果
     */
    @Override
    public int deleteHospitalInfoByIds(Long[] ids)
    {
        HospitalInfo[] oldValues = new HospitalInfo[ids.length];
        for (int i = 0; i < ids.length; i++)
        {
            oldValues[i] = hospitalInfoMapper.selectHospitalInfoById(ids[i]);
        }
        int result = hospitalInfoMapper.deleteHospitalInfoByIds(ids);
        if (result > 0)
        {
            for (int i = 0; i < ids.length; i++)
            {
                basicInfoCache.evictAfterCommit(idKey(ids[i]),
                        oldValues[i] == null ? null : codeKey(oldValues[i].getHospitalCode()));
            }
        }
        return result;
    }

    /**
     * 删除医院管理信息
     *
     * @param id 医院管理主键
     * @return 结果
     */
    @Override
    public int deleteHospitalInfoById(Long id)
    {
        HospitalInfo old = hospitalInfoMapper.selectHospitalInfoById(id);
        int result = hospitalInfoMapper.deleteHospitalInfoById(id);
        if (result > 0)
        {
            basicInfoCache.evictAfterCommit(idKey(id), old == null ? null : codeKey(old.getHospitalCode()));
        }
        return result;
    }

    @Override
    public List<HospitalInfo> getList() {
        return hospitalInfoMapper.selectList();
    }

    @Override
    public HospitalInfo selectHospitalInfoByCode(String hoscode) {
        return basicInfoCache.getOrLoad(codeKey(hoscode), HospitalInfo.class,
                () -> hospitalInfoMapper.selectHospitalInfoByCode(hoscode));
    }

    private String idKey(Long id) {
        return id == null ? null : CACHE_ID_PREFIX + id;
    }

    private String codeKey(String code) {
        return code == null ? null : CACHE_CODE_PREFIX + code;
    }


}
