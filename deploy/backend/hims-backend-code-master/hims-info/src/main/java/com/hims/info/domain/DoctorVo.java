package com.hims.info.domain;

import com.hims.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 医生信息对象 doctor_info
 */
@Data
public class DoctorVo extends BaseEntity {
    private Long userId;

    private Long deptId;

    private String dept;

    private String userName;

    private String nickName;

    private Long hospitalId;

    private String hospitalName;

    private String email;

    private String phonenumber;

    private String sex;

    private String status;
}
