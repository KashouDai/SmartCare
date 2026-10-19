package com.hims.info.service.impl;

import java.util.List;

import com.hims.info.domain.DrugInfo;
import com.hims.info.mapper.DrugInfoMapper;
import com.hims.info.service.IDrugInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 药品基础信息Service业务层处理
 */
@Service
public class DrugInfoServiceImpl implements IDrugInfoService
{
    @Autowired
    private DrugInfoMapper drugInfoMapper;

    /**
     * 查询药品基础信息
     *
     * @param id 药品基础信息主键
     * @return 药品基础信息
     */
    @Override
    public DrugInfo selectDrugInfoById(Long id)
    {
        return drugInfoMapper.selectDrugInfoById(id);
    }

    /**
     * 查询药品基础信息列表
     *
     * @param drugInfo 药品基础信息
     * @return 药品基础信息
     */
    @Override
    public List<DrugInfo> selectDrugInfoList(DrugInfo drugInfo)
    {
        return drugInfoMapper.selectDrugInfoList(drugInfo);
    }

    /**
     * 新增药品基础信息
     *
     * @param drugInfo 药品基础信息
     * @return 结果
     */
    @Override
    public int insertDrugInfo(DrugInfo drugInfo)
    {
        return drugInfoMapper.insertDrugInfo(drugInfo);
    }

    /**
     * 修改药品基础信息
     *
     * @param drugInfo 药品基础信息
     * @return 结果
     */
    @Override
    public int updateDrugInfo(DrugInfo drugInfo)
    {
        return drugInfoMapper.updateDrugInfo(drugInfo);
    }

    /**
     * 批量删除药品基础信息
     *
     * @param ids 需要删除的药品基础信息主键
     * @return 结果
     */
    @Override
    public int deleteDrugInfoByIds(Long[] ids)
    {
        return drugInfoMapper.deleteDrugInfoByIds(ids);
    }

    /**
     * 删除药品基础信息信息
     *
     * @param id 药品基础信息主键
     * @return 结果
     */
    @Override
    public int deleteDrugInfoById(Long id)
    {
        return drugInfoMapper.deleteDrugInfoById(id);
    }
}
