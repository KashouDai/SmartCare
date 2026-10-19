package com.hims.info.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hims.common.annotation.Excel;
import com.hims.common.core.domain.BaseEntity;

/**
 * 就诊历史对象 medical_history
 */
@Data
public class MedicalHistory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** id */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long historyId;

    /** 就诊卡 */
    private String cardNo;

    /** 就诊日期 */
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    @Excel(name = "就诊日期", width = 30, dateFormat = "yyyy-MM-dd hh:mm:ss")
    private Date visitDate;

    /** 医生姓名 */
    @Excel(name = "医生姓名")
    private String doctorName;

    /** 诊断结果 */
    @Excel(name = "诊断结果")
    private String diagnosis;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

}
