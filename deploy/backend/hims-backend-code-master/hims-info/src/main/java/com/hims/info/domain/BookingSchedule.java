package com.hims.info.domain;

import lombok.Data;


@Data
public class BookingSchedule {
    /**
     * 日期
     */
    private String date;

    /**
     * 周几
     */
    private String weekday;

    /**
     * 状态 0-在预约时间内， 1-即将放号 2-停止挂号
     */
    private String state;

    /**
     * 盈余状态 0-无号 1-有号
     */
    private String surplusState;


}
