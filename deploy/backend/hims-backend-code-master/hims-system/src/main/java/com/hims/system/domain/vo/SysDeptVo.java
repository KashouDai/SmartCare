package com.hims.system.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 部门信息vo
 **/
@Data
public class SysDeptVo {
    private String deptId;

    private String deptName;

    private List<SysDeptVo> children;
}
