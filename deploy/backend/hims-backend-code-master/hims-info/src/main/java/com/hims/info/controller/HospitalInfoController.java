package com.hims.info.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.hims.info.domain.HospitalInfo;
import com.hims.info.service.IHospitalInfoService;
import com.hims.common.annotation.Anonymous;
import com.hims.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hims.common.annotation.Log;
import com.hims.common.core.controller.BaseController;
import com.hims.common.core.domain.AjaxResult;
import com.hims.common.enums.BusinessType;
import com.hims.common.utils.poi.ExcelUtil;
import com.hims.common.core.page.TableDataInfo;

/**
 * 医院管理Controller
 */
@RestController
@RequestMapping("/info/hospital")
public class HospitalInfoController extends BaseController
{
    @Autowired
    private IHospitalInfoService hospitalInfoService;

    /**
     * 查询医院管理列表
     */
//    @PreAuthorize("@ss.hasPermi('info:hospital:list')")
    @Anonymous
    @GetMapping("/list")
    public TableDataInfo list(HospitalInfo hospitalInfo)
    {
        startPage();
        List<HospitalInfo> list = hospitalInfoService.selectHospitalInfoList(hospitalInfo);
        return getDataTable(list);
    }

    /**
     * 导出医院管理列表
     */
    @PreAuthorize("@ss.hasPermi('info:hospital:export')")
    @Log(title = "医院管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HospitalInfo hospitalInfo)
    {
        List<HospitalInfo> list = hospitalInfoService.selectHospitalInfoList(hospitalInfo);
        ExcelUtil<HospitalInfo> util = new ExcelUtil<HospitalInfo>(HospitalInfo.class);
        util.exportExcel(response, list, "医院管理数据");
    }

    /**
     * 获取医院管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('info:hospital:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(hospitalInfoService.selectHospitalInfoById(id));
    }

    /**
     * 新增医院管理
     */
    @PreAuthorize("@ss.hasPermi('info:hospital:add')")
    @Log(title = "医院管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HospitalInfo hospitalInfo)
    {
        return toAjax(hospitalInfoService.insertHospitalInfo(hospitalInfo));
    }

    /**
     * 修改医院管理
     */
    @PreAuthorize("@ss.hasPermi('info:hospital:edit')")
    @Log(title = "医院管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HospitalInfo hospitalInfo)
    {
        return toAjax(hospitalInfoService.updateHospitalInfo(hospitalInfo));
    }

    /**
     * 删除医院管理
     */
    @PreAuthorize("@ss.hasPermi('info:hospital:remove')")
    @Log(title = "医院管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(hospitalInfoService.deleteHospitalInfoByIds(ids));
    }

    /**
     * 获取医院下拉列表
     */
    @PreAuthorize("@ss.hasPermi('info:hospital:list')")
    @GetMapping("/getList")
    public R<List<HospitalInfo>> getList()
    {
        return R.ok(hospitalInfoService.getList());
    }

    /**
     * 获取医院管理详细信息
     */
    @Anonymous
    @GetMapping(value = "/getDetail/{hoscode}")
    public AjaxResult getDetailByCode(@PathVariable("hoscode") String hoscode) {
        return success(hospitalInfoService.selectHospitalInfoByCode(hoscode));
    }
}
