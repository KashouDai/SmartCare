package com.hims.info.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.hims.info.domain.DutyInfo;
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
import com.hims.info.service.IDutyInfoService;
import com.hims.common.utils.poi.ExcelUtil;
import com.hims.common.core.page.TableDataInfo;

/**
 * 排班信息Controller
 */
@RestController
@RequestMapping("/info/duty")
public class DutyInfoController extends BaseController
{
    @Autowired
    private IDutyInfoService dutyInfoService;

    /**
     * 查询排班信息列表
     */
    @PreAuthorize("@ss.hasPermi('info:duty:list')")
    @GetMapping("/list")
    public TableDataInfo list(DutyInfo dutyInfo) {
        startPage();
        List<DutyInfo> list = dutyInfoService.selectDutyInfoList(dutyInfo);
        return getDataTable(list);
    }

    /**
     * 导出排班信息列表
     */
    @PreAuthorize("@ss.hasPermi('info:duty:export')")
    @Log(title = "排班信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DutyInfo dutyInfo)
    {
        List<DutyInfo> list = dutyInfoService.selectDutyInfoList(dutyInfo);
        ExcelUtil<DutyInfo> util = new ExcelUtil<DutyInfo>(DutyInfo.class);
        util.exportExcel(response, list, "排班信息数据");
    }

    /**
     * 获取排班信息详细信息
     */
//    @PreAuthorize("@ss.hasPermi('info:duty:query')")
    @GetMapping(value = "/{scheduleId}")
    public AjaxResult getInfo(@PathVariable("scheduleId") Long scheduleId)
    {
        return success(dutyInfoService.selectDutyInfoByScheduleId(scheduleId));
    }

    /**
     * 新增排班信息
     */
    @PreAuthorize("@ss.hasPermi('info:duty:add')")
    @Log(title = "排班信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DutyInfo dutyInfo)
    {
        return toAjax(dutyInfoService.insertDutyInfo(dutyInfo));
    }

    /**
     * 修改排班信息
     */
    @PreAuthorize("@ss.hasPermi('info:duty:edit')")
    @Log(title = "排班信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DutyInfo dutyInfo)
    {
        return toAjax(dutyInfoService.updateDutyInfo(dutyInfo));
    }

    /**
     * 删除排班信息
     */
    @PreAuthorize("@ss.hasPermi('info:duty:remove')")
    @Log(title = "排班信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{scheduleIds}")
    public AjaxResult remove(@PathVariable Long[] scheduleIds)
    {
        return toAjax(dutyInfoService.deleteDutyInfoByScheduleIds(scheduleIds));
    }



    /**
     * 获取值班医生信息
     */
    @Anonymous
    @GetMapping("/getDutyDoctorInfo")
    public R<List<DutyInfo>> getDutyDoctorInfo(DutyInfo dutyInfo){
        return R.ok(dutyInfoService.selectDutyDoctorInfo(dutyInfo));
    }


}
