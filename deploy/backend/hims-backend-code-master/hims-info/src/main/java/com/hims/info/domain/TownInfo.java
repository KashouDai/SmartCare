package com.hims.info.domain;

import com.hims.common.annotation.Excel;
import com.hims.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 区县信息对象 town_info
 */
@Data
public class TownInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 区id */
    private Long tid;

    /**
     * 区县编码
     */
    private String tCode;

    /** 区县名称 */
    private String tName;

    /** 市编号 */
    private String cCode;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;
}
