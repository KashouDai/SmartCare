package com.hims.info.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hims.common.annotation.Excel;
import com.hims.common.core.domain.BaseEntity;

/**
 * 城市信息对象 city_info
 */
@Data
public class CityInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 市id */
    private Long cid;

    /**
     * 市编码
     */
    private String cCode;

    /** 市名称 */
    private String cName;

    /** 省id */
    private String pCode;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;
}
