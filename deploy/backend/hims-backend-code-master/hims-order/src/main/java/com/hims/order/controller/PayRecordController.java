package com.hims.order.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.hims.order.domain.PayRecord;
import com.hims.order.service.IPayRecordService;
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
 * 支付信息Controller
 */
@RestController
@RequestMapping("/order/record")
public class PayRecordController extends BaseController
{
    @Autowired
    private IPayRecordService payRecordService;

    /**
     * 查询支付信息列表
     */
    @PreAuthorize("@ss.hasPermi('order:record:list')")
    @GetMapping("/list")
    public TableDataInfo list(PayRecord payRecord)
    {
        startPage();
        List<PayRecord> list = payRecordService.selectPayRecordList(payRecord);
        return getDataTable(list);
    }

    /**
     * 导出支付信息列表
     */
    @PreAuthorize("@ss.hasPermi('order:record:export')")
    @Log(title = "支付信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PayRecord payRecord)
    {
        List<PayRecord> list = payRecordService.selectPayRecordList(payRecord);
        ExcelUtil<PayRecord> util = new ExcelUtil<PayRecord>(PayRecord.class);
        util.exportExcel(response, list, "支付信息数据");
    }

    /**
     * 获取支付信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('order:record:query')")
    @GetMapping(value = "/{tradeNo}")
    public AjaxResult getInfo(@PathVariable("tradeNo") String tradeNo)
    {
        return success(payRecordService.selectPayRecordByTradeNo(tradeNo));
    }

    /**
     * 新增支付信息
     */
    @PreAuthorize("@ss.hasPermi('order:record:add')")
    @Log(title = "支付信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PayRecord payRecord)
    {
        return toAjax(payRecordService.insertPayRecord(payRecord));
    }

    /**
     * 修改支付信息
     */
    @PreAuthorize("@ss.hasPermi('order:record:edit')")
    @Log(title = "支付信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PayRecord payRecord)
    {
        return toAjax(payRecordService.updatePayRecord(payRecord));
    }

    /**
     * 删除支付信息
     */
    @PreAuthorize("@ss.hasPermi('order:record:remove')")
    @Log(title = "支付信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{tradeNos}")
    public AjaxResult remove(@PathVariable String[] tradeNos)
    {
        return toAjax(payRecordService.deletePayRecordByTradeNos(tradeNos));
    }
}
