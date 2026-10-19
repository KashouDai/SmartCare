package com.hims.info.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hims.common.annotation.Excel;
import com.hims.common.core.domain.BaseEntity;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 预约信息对象 appointments_info
 */
@Data
public class AppointmentsInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 预约id */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long appointmentsId;

    /**
     * 排班id
     */
    private Long scheduleId;

    /** 医生id */
    private Long doctorId;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 就诊卡号
     */
    private String cardNo;

    /** 患者id */
    private Long patientId;
    /**
     * 患者姓名
     */
    private String patientName;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    /**
     * 就诊时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date visitTime;

    /** 值班时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "值班时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date dutyDate;

    /** 值班时段 */
    @Excel(name = "值班时段")
    private String dutyPeriod;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

}
