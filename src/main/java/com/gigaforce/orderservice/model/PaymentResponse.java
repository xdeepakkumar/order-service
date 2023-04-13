package com.gigaforce.orderservice.model;

import com.gigaforce.orderservice.enums.PaymentMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {
    private Long paymentId;
    private PaymentMode paymentMode;
    private Long amount;
    private Instant paymentDate;
    private long orderId;
    private String status;
}
