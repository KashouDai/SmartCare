package com.hims.order.domain;

import lombok.Data;

@Data
public class RegisteredOrderRequest {
    private Long scheduleId;
    private Long patientId;
    private String requestId;
}
