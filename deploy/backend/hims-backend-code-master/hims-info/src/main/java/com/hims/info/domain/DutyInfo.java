package com.hims.info.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.hims.common.annotation.Excel;
import com.hims.common.core.domain.BaseEntity;

/**
 * 排班信息对象 duty_info
 */
@Data
public class DutyInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 排班ID，主键 */
    private Long scheduleId;

    /** 医生ID */
    private Long doctorId;

    /**
     * 医生姓名
     */
    private String doctorName;

    private String hospitalId;
    /**
     * 医院名称
     */
    private String hospitalName;

    private String deptId;
    /**
     * 所属科室
     */
    private String deptName;

    /** 值班时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "值班时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date dutyDate;

    /** 值班时段 */
    @Excel(name = "值班时段")
    private String dutyPeriod;

    /** 可预约数 */
    @Excel(name = "可预约数")
    private Long appointmentsNum;

    /** 已预约数 */
    @Excel(name = "已预约数")
    private Long appointmentsNumMade;

    /** 挂号费 */
    @Excel(name = "挂号费")
    private BigDecimal registrationFee;

    /**
     * 医生介绍
     */
    private String doctorIntroduction;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

}
