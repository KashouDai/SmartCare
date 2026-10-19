package com.hims.info.service;


import com.hims.info.domain.CityInfo;
import com.hims.info.domain.ProvinceInfo;
import com.hims.info.domain.TownInfo;

import java.util.List;

/**
 * 城市信息Service接口
 */
public interface ICityInfoService
{

    List<ProvinceInfo> getProvinceList();

    List<CityInfo> getCityListByPCode(String provinceCode);

    List<TownInfo> getTownListByCCode(String cityCode);
}
