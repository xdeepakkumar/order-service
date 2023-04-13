package com.gigaforce.orderservice.external.client;

import com.gigaforce.orderservice.request.PaymentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "PAYMENT-SERVICE/payment")
public interface PaymentService {

    @PostMapping("/doPayment")
    ResponseEntity<Long> doPayment(@RequestBody PaymentRequest paymentRequest);
}
