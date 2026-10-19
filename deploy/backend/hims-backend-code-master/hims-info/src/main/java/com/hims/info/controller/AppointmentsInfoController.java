package com.hims.info.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.hims.info.domain.AppointmentsInfo;
import com.hims.info.domain.BookingSchedule;
import com.hims.info.domain.BookingScheduleBo;
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
import com.hims.info.service.IAppointmentsInfoService;
import com.hims.common.utils.poi.ExcelUtil;
import com.hims.common.core.page.TableDataInfo;

/**
 * 预约信息Controller
 */
@RestController
@RequestMapping("/info/appointments")
public class AppointmentsInfoController extends BaseController
{
    @Autowired
    private IAppointmentsInfoService appointmentsInfoService;

    /**
     * 查询预约信息列表
     */
    @PreAuthorize("@ss.hasPermi('info:appointments:list')")
    @GetMapping("/list")
    public TableDataInfo list(AppointmentsInfo appointmentsInfo)
    {
        startPage();
        List<AppointmentsInfo> list = appointmentsInfoService.selectAppointmentsInfoList(appointmentsInfo);
        return getDataTable(list);
    }

    /**
     * 导出预约信息列表
     */
    @PreAuthorize("@ss.hasPermi('info:appointments:export')")
    @Log(title = "预约信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AppointmentsInfo appointmentsInfo)
    {
        List<AppointmentsInfo> list = appointmentsInfoService.selectAppointmentsInfoList(appointmentsInfo);
        ExcelUtil<AppointmentsInfo> util = new ExcelUtil<AppointmentsInfo>(AppointmentsInfo.class);
        util.exportExcel(response, list, "预约信息数据");
    }

    /**
     * 获取预约信息详细信息
     */
//    @PreAuthorize("@ss.hasPermi('info:appointments:query')")
    @GetMapping(value = "/{appointmentsId}")
    public AjaxResult getInfo(@PathVariable("appointmentsId") Long appointmentsId)
    {
        return success(appointmentsInfoService.selectAppointmentsInfoByAppointmentsId(appointmentsId));
    }

    /**
     * 新增预约信息
     * 仅管理端可用：患者端预约一律走统一事务入口 POST /order/orderInfo/registeredOrder。
     * 本接口使用非原子占号、无患者归属校验，且预约和订单不在统一事务中，可绕过统一下单链路，
     * 放开会给非管理端留下绕过统一事务的写入路径。
     */
    @PreAuthorize("@ss.hasPermi('info:appointments:add')")
    @Log(title = "预约信息", businessType = BusinessType.INSERT)
    @PostMapping
    public R<String> add(@RequestBody AppointmentsInfo appointmentsInfo)
    {
        return R.ok(appointmentsInfoService.insertAppointmentsInfo(appointmentsInfo));
    }

    /**
     * 修改预约信息
     */
//    @PreAuthorize("@ss.hasPermi('info:appointments:edit')")
    @Log(title = "预约信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AppointmentsInfo appointmentsInfo)
    {
        return toAjax(appointmentsInfoService.updateAppointmentsInfo(appointmentsInfo));
    }

    /**
     * 删除预约信息
     */
//    @PreAuthorize("@ss.hasPermi('info:appointments:remove')")
    @Log(title = "预约信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{appointmentsIds}")
    public AjaxResult remove(@PathVariable Long[] appointmentsIds)
    {
        return toAjax(appointmentsInfoService.deleteAppointmentsInfoByAppointmentsIds(appointmentsIds));
    }

    /**
     * 获取预订时间表列表
     */
    @Anonymous
    @GetMapping("/getBookingScheduleList")
    public TableDataInfo getBookingScheduleList(BookingScheduleBo bo){
        startPage();
        List<BookingSchedule> list = appointmentsInfoService.getBookingScheduleList(bo);
        return getDataTable(list);
    }
}
