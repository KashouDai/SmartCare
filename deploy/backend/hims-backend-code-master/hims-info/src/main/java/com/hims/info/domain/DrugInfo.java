package com.hims.info.domain;

import java.math.BigDecimal;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hims.common.annotation.Excel;
import com.hims.common.core.domain.BaseEntity;

/**
 * 药品基础信息对象 drug_info
 */
@Data
public class DrugInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** id */
    private Long id;

    /** 药品名称 */
    @Excel(name = "药品名称")
    private String drugName;

    /** 生产厂家 */
    @Excel(name = "生产厂家")
    private String manufacturer;

    /** 药品剂型 */
    @Excel(name = "药品剂型")
    private String dosageForm;

    /** 当前库存量 */
    @Excel(name = "当前库存量")
    private Long stockQuantity;

    /** 进货价格 */
    @Excel(name = "进货价格")
    private BigDecimal purchasePrice;

    /** 零售价格 */
    @Excel(name = "零售价格")
    private BigDecimal retailPrice;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;
}
