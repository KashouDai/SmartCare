package com.hims.info.domain;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hims.common.annotation.Excel;
import com.hims.common.core.domain.BaseEntity;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 医院管理对象 hospital_set
 */
@Data
public class HospitalInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 编号 */
    private Long id;

    /** 医院名称 */
    private String hospitalName;

    /** 医院编号 */
    private String hospitalCode;

    private Integer appointmentDay;

    private String pcode;

    /**
     * 省份
     */
    @Excel(name = "省份")
    private String pName;

    private String ccode;

    /**
     * 城市
     */
    private String cName;

    private String tcode;

    private String tName;

    /**
     * 等级
     */
    private String level;

    /**
     * 放号时间
     */
    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date releaseTime;
    /**
     * 停挂时间
     */
    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date suspendTime;
    /** 联系人 */
    private String contactsName;

    /** 联系人手机 */
    private String contactsPhone;

    /** 状态 0-可用 1-不可用 */
    @Excel(name = "状态")
    private Long status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;


}
