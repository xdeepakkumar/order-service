package com.gigaforce.orderservice.service;

import com.gigaforce.orderservice.model.OrderRequest;

public interface OrderService {
    Long placeOrder (OrderRequest orderRequest);
}
