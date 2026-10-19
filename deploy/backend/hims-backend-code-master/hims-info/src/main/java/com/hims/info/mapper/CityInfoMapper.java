package com.hims.info.mapper;

import java.util.List;
import com.hims.info.domain.CityInfo;
import com.hims.info.domain.ProvinceInfo;
import com.hims.info.domain.TownInfo;

/**
 * 城市信息Mapper接口
 */
public interface CityInfoMapper
{

    /**
     * 获取省份列表
     */
    List<ProvinceInfo> getProvinceList();

    /**
     * 按p代码获取城市列表
     */
    List<CityInfo> getCityListByPCode(String provinceCode);

    /**
     * 按C代码获取城镇列表
     */
    List<TownInfo> getTownListByCCode(String cityCode);
}
