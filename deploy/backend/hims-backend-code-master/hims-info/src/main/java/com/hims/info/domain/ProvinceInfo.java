package com.hims.info.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hims.common.annotation.Excel;
import com.hims.common.core.domain.BaseEntity;

/**
 * 省份信息对象 province_info
 */
@Data
public class ProvinceInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 省id */
    private Long pid;
    private String pCode;

    /** 省名称 */
    @Excel(name = "省名称")
    private String pName;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

}
