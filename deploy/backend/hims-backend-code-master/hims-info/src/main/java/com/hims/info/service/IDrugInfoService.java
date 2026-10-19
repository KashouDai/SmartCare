package com.hims.info.service;

import java.util.List;

import com.hims.info.domain.DrugInfo;

/**
 * 药品基础信息Service接口
 */
public interface IDrugInfoService
{
    /**
     * 查询药品基础信息
     *
     * @param id 药品基础信息主键
     * @return 药品基础信息
     */
    public DrugInfo selectDrugInfoById(Long id);

    /**
     * 查询药品基础信息列表
     *
     * @param drugInfo 药品基础信息
     * @return 药品基础信息集合
     */
    public List<DrugInfo> selectDrugInfoList(DrugInfo drugInfo);

    /**
     * 新增药品基础信息
     *
     * @param drugInfo 药品基础信息
     * @return 结果
     */
    public int insertDrugInfo(DrugInfo drugInfo);

    /**
     * 修改药品基础信息
     *
     * @param drugInfo 药品基础信息
     * @return 结果
     */
    public int updateDrugInfo(DrugInfo drugInfo);

    /**
     * 批量删除药品基础信息
     *
     * @param ids 需要删除的药品基础信息主键集合
     * @return 结果
     */
    public int deleteDrugInfoByIds(Long[] ids);

    /**
     * 删除药品基础信息信息
     *
     * @param id 药品基础信息主键
     * @return 结果
     */
    public int deleteDrugInfoById(Long id);
}
