package com.gigaforce.orderservice.external.client;

import com.gigaforce.orderservice.exception.CustomException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@CircuitBreaker(name = "external", fallbackMethod = "fallback")
@FeignClient(name = "PRODUCT-SERVICE/product")
public interface ProductService {

    @PutMapping("/reduceQuantity/{id}")
    void reduceQuantity (@PathVariable(name = "id") Long productId, @RequestParam Long quantity);

    default void fallback(Exception e){
        throw new CustomException("Product service is not available", "UNAVAILABLE", 500);
    }
}
