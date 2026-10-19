package com.hims.info.domain;

import com.hims.common.core.domain.BaseEntity;
import lombok.Data;
import org.springframework.web.bind.annotation.PathVariable;


@Data
public class BookingScheduleBo extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private String hospitalCode;
    private String deptId;
}
