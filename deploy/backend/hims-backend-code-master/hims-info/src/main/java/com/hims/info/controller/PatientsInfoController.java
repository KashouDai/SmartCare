package com.hims.info.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.hims.info.domain.PatientsInfo;
import com.hims.info.service.IPatientsInfoService;
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
import com.hims.common.utils.SecurityUtils;
import com.hims.common.utils.poi.ExcelUtil;
import com.hims.common.core.page.TableDataInfo;

/**
 * 患者信息Controller
 */
@RestController
@RequestMapping("/info/patientsInfo")
public class PatientsInfoController extends BaseController
{
    @Autowired
    private IPatientsInfoService patientsInfoService;

    /**
     * 查询患者信息列表
     */
    @PreAuthorize("@ss.hasPermi('info:patientsInfo:list')")
    @GetMapping("/list")
    public TableDataInfo list(PatientsInfo patientsInfo)
    {
        startPage();
        List<PatientsInfo> list = patientsInfoService.selectPatientsInfoList(patientsInfo);
        return getDataTable(list);
    }

    /**
     * 导出患者信息列表
     */
    @PreAuthorize("@ss.hasPermi('info:patientsInfo:export')")
    @Log(title = "患者信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PatientsInfo patientsInfo)
    {
        List<PatientsInfo> list = patientsInfoService.selectPatientsInfoList(patientsInfo);
        ExcelUtil<PatientsInfo> util = new ExcelUtil<PatientsInfo>(PatientsInfo.class);
        util.exportExcel(response, list, "患者信息数据");
    }

    /**
     * 获取患者信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('info:patientsInfo:query')")
    @GetMapping(value = "/{patientId}")
    public AjaxResult getInfo(@PathVariable("patientId") Long patientId)
    {
        return success(patientsInfoService.selectPatientsInfoByPatientId(patientId));
    }

    /**
     * 查询当前登录用户绑定的患者列表
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mine")
    public AjaxResult mine()
    {
        return success(patientsInfoService.selectPatientsByUserId(SecurityUtils.getUserId()));
    }

    /**
     * 新增患者信息
     */
//    @PreAuthorize("@ss.hasPermi('info:patientsInfo:add')")
    @Log(title = "患者信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PatientsInfo patientsInfo)
    {
        return toAjax(patientsInfoService.insertPatientsInfo(patientsInfo));
    }

    /**
     * 修改患者信息
     */
    @PreAuthorize("@ss.hasPermi('info:patientsInfo:edit')")
    @Log(title = "患者信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PatientsInfo patientsInfo)
    {
        return toAjax(patientsInfoService.updatePatientsInfo(patientsInfo));
    }

    /**
     * 删除患者信息
     */
    @PreAuthorize("@ss.hasPermi('info:patientsInfo:remove')")
    @Log(title = "患者信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{patientIds}")
    public AjaxResult remove(@PathVariable Long[] patientIds)
    {
        return toAjax(patientsInfoService.deletePatientsInfoByPatientIds(patientIds));
    }
}
