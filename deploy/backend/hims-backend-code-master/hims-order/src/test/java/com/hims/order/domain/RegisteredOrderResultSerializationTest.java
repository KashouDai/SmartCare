package com.hims.order.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegisteredOrderResultSerializationTest {

    @Test
    void serializesLargeIdsAsStrings() throws Exception {
        RegisteredOrderResult result = new RegisteredOrderResult();
        result.setOrderId(1558296995172470784L);
        result.setAppointmentsId(1558296995159887872L);

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(result));

        assertEquals("1558296995172470784", json.get("orderId").asText());
        assertEquals("1558296995159887872", json.get("appointmentsId").asText());
    }
}
