package com.hims.common.core.domain.entity;

import lombok.Data;

/**
 * 用户信息修改bo
 **/
@Data
public class SysUserBo {
    private String userName;

    private String phonenumber;

    private String oldPassword;

    private String password;
}
