package com.hims.info.controller;

import com.hims.info.domain.DoctorVo;
import com.hims.common.core.controller.BaseController;
import com.hims.common.core.domain.AjaxResult;
import com.hims.common.core.domain.R;
import com.hims.common.core.domain.entity.SysUser;
import com.hims.common.core.page.TableDataInfo;
import com.hims.common.utils.SecurityUtils;
import com.hims.info.service.DoctorService;
import com.hims.system.service.ISysPostService;
import com.hims.system.service.ISysRoleService;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医生信息管理Controller
 */
@RestController
@RequestMapping("/info/doctor")
public class DoctorController extends BaseController {
    @Autowired
    private DoctorService doctorService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysPostService postService;

    /**
     * 查询医生信息管理列表
     */
    @GetMapping("/list")
    public TableDataInfo list(DoctorVo sysUser) {
        startPage();
        List<DoctorVo> list = doctorService.selectUserList(sysUser);
        return getDataTable(list);
    }

    @PostMapping()
    public AjaxResult add(@RequestBody SysUser user) {
        // 校验是否可以新增
        doctorService.check(user);
        user.setCreateBy(getUsername());
        user.setRoleIds(new Long[]{2L});
        user.setStatus("0");
        user.setPassword("<configured-at-runtime>");
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(doctorService.insertUser(user));
    }

    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds){
        if (ArrayUtils.contains(userIds, getUserId()))
        {
            return error("当前用户不能删除");
        }
        return toAjax(doctorService.deleteUserByIds(userIds));
    }

    @GetMapping(value = { "/", "/{userId}" })
    public R<DoctorVo> getInfo(@PathVariable(value = "userId", required = false) Long userId){
        return R.ok(doctorService.selectUserById(userId));
    }
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody DoctorVo doctorVo) {
        doctorVo.setUpdateBy(getUsername());
        return toAjax(doctorService.updateUser(doctorVo));
    }


}
