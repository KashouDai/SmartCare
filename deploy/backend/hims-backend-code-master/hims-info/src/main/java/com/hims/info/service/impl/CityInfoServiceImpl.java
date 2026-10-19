package com.hims.info.service.impl;

import java.util.List;

import com.hims.info.domain.CityInfo;
import com.hims.info.domain.ProvinceInfo;
import com.hims.info.domain.TownInfo;
import com.hims.info.mapper.CityInfoMapper;
import com.hims.info.service.ICityInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 城市信息Service业务层处理
 */
@Service
public class CityInfoServiceImpl implements ICityInfoService
{
    @Autowired
    private CityInfoMapper cityInfoMapper;


    @Override
    public List<ProvinceInfo> getProvinceList() {
        return cityInfoMapper.getProvinceList();
    }

    @Override
    public List<CityInfo> getCityListByPCode(String provinceCode) {
        return cityInfoMapper.getCityListByPCode(provinceCode);
    }

    @Override
    public List<TownInfo> getTownListByCCode(String cityCode) {
        return cityInfoMapper.getTownListByCCode(cityCode);
    }
}
