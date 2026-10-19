package com.hims.common.core.domain.model;

import lombok.Data;

/**
 * 用户注册对象
 *
 *  
 */
@Data
public class RegisterBody {
    private String userName;

    private String password;

    private String nickName;

    private String sex;

    private String phone;

    private String idCard;
}
