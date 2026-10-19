package com.hims.info.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.hims.info.domain.MedicalHistory;
import com.hims.info.service.IMedicalHistoryService;
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
 * 就诊历史Controller
 */
@RestController
@RequestMapping("/info/history")
public class MedicalHistoryController extends BaseController
{
    @Autowired
    private IMedicalHistoryService medicalHistoryService;

    /**
     * 查询就诊历史列表
     */
    @PreAuthorize("@ss.hasPermi('info:history:list')")
    @GetMapping("/list")
    public TableDataInfo list(MedicalHistory medicalHistory)
    {
        startPage();
        List<MedicalHistory> list = medicalHistoryService.selectMedicalHistoryList(medicalHistory);
        return getDataTable(list);
    }

    /**
     * 导出就诊历史列表
     */
    @PreAuthorize("@ss.hasPermi('info:history:export')")
    @Log(title = "就诊历史", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MedicalHistory medicalHistory)
    {
        List<MedicalHistory> list = medicalHistoryService.selectMedicalHistoryList(medicalHistory);
        ExcelUtil<MedicalHistory> util = new ExcelUtil<MedicalHistory>(MedicalHistory.class);
        util.exportExcel(response, list, "就诊历史数据");
    }

    /**
     * 获取就诊历史详细信息
     */
    @PreAuthorize("@ss.hasPermi('info:history:query')")
    @GetMapping(value = "/{historyId}")
    public AjaxResult getInfo(@PathVariable("historyId") Long historyId)
    {
        return success(medicalHistoryService.selectMedicalHistoryByHistoryId(historyId));
    }

    /**
     * 新增就诊历史
     */
    @PreAuthorize("@ss.hasPermi('info:history:add')")
    @Log(title = "就诊历史", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MedicalHistory medicalHistory)
    {
        return toAjax(medicalHistoryService.insertMedicalHistory(medicalHistory));
    }

    /**
     * 修改就诊历史
     */
    @PreAuthorize("@ss.hasPermi('info:history:edit')")
    @Log(title = "就诊历史", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MedicalHistory medicalHistory)
    {
        return toAjax(medicalHistoryService.updateMedicalHistory(medicalHistory));
    }

    /**
     * 删除就诊历史
     */
    @PreAuthorize("@ss.hasPermi('info:history:remove')")
    @Log(title = "就诊历史", businessType = BusinessType.DELETE)
	@DeleteMapping("/{historyIds}")
    public AjaxResult remove(@PathVariable Long[] historyIds)
    {
        return toAjax(medicalHistoryService.deleteMedicalHistoryByHistoryIds(historyIds));
    }
}
