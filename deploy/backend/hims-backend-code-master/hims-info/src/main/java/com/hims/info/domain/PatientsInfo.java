package com.hims.info.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hims.common.annotation.Excel;
import com.hims.common.core.domain.BaseEntity;
import org.springframework.data.annotation.Id;

/**
 * 患者信息对象 patients_info
 */
@Data
public class PatientsInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    /** 患者id */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long patientId;

    /** 就诊卡号 */
    @Excel(name = "就诊卡号")
    private String cardNo;

    /** 患者姓名 */
    @Excel(name = "患者姓名")
    private String nickName;

    /** 患者性别 */
    @Excel(name = "患者性别")
    private String sex;

    /** 手机号码 */
    @Excel(name = "手机号码")
    private String phone;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

}
