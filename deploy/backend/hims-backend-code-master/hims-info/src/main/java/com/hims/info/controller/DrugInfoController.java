package com.hims.info.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.hims.info.domain.DrugInfo;
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
import com.hims.info.service.IDrugInfoService;
import com.hims.common.utils.poi.ExcelUtil;
import com.hims.common.core.page.TableDataInfo;

/**
 * 药品基础信息Controller
 */
@RestController
@RequestMapping("/info/drugInfo")
public class DrugInfoController extends BaseController
{
    @Autowired
    private IDrugInfoService drugInfoService;

    /**
     * 查询药品基础信息列表
     */
    @PreAuthorize("@ss.hasPermi('info:drugInfo:list')")
    @GetMapping("/list")
    public TableDataInfo list(DrugInfo drugInfo)
    {
        startPage();
        List<DrugInfo> list = drugInfoService.selectDrugInfoList(drugInfo);
        return getDataTable(list);
    }

    /**
     * 导出药品基础信息列表
     */
    @PreAuthorize("@ss.hasPermi('info:drugInfo:export')")
    @Log(title = "药品基础信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DrugInfo drugInfo)
    {
        List<DrugInfo> list = drugInfoService.selectDrugInfoList(drugInfo);
        ExcelUtil<DrugInfo> util = new ExcelUtil<DrugInfo>(DrugInfo.class);
        util.exportExcel(response, list, "药品基础信息数据");
    }

    /**
     * 获取药品基础信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('info:drugInfo:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(drugInfoService.selectDrugInfoById(id));
    }

    /**
     * 新增药品基础信息
     */
    @PreAuthorize("@ss.hasPermi('info:drugInfo:add')")
    @Log(title = "药品基础信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DrugInfo drugInfo)
    {
        return toAjax(drugInfoService.insertDrugInfo(drugInfo));
    }

    /**
     * 修改药品基础信息
     */
    @PreAuthorize("@ss.hasPermi('info:drugInfo:edit')")
    @Log(title = "药品基础信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DrugInfo drugInfo)
    {
        return toAjax(drugInfoService.updateDrugInfo(drugInfo));
    }

    /**
     * 删除药品基础信息
     */
    @PreAuthorize("@ss.hasPermi('info:drugInfo:remove')")
    @Log(title = "药品基础信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(drugInfoService.deleteDrugInfoByIds(ids));
    }
}
