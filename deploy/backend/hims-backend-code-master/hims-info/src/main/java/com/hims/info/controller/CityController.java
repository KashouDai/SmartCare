package com.hims.info.controller;

import com.hims.info.domain.CityInfo;
import com.hims.info.domain.ProvinceInfo;
import com.hims.info.domain.TownInfo;
import com.hims.common.annotation.Anonymous;
import com.hims.common.core.domain.R;
import com.hims.info.service.ICityInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 级联查询
 **/
@RestController
@RequestMapping("/info/city")
public class CityController {
    @Autowired
    private ICityInfoService cityInfoService;

    @GetMapping()
    @Anonymous
    public R<List<ProvinceInfo>> getProvinceList(){
        return R.ok(cityInfoService.getProvinceList());
    }

    @GetMapping("/city/{provinceCode}")
    @Anonymous
    public R<List<CityInfo>> getCityListByPCode(@PathVariable String provinceCode){
        return R.ok(cityInfoService.getCityListByPCode(provinceCode));
    }

    @GetMapping("/town/{cityCode}")
    @Anonymous
    public R<List<TownInfo>> getTownListByCCode(@PathVariable String cityCode){
        return R.ok(cityInfoService.getTownListByCCode(cityCode));
    }
}
