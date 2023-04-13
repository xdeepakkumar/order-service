package com.gigaforce.orderservice.service;

import com.gigaforce.orderservice.model.OrderRequest;
import com.gigaforce.orderservice.model.OrderResponse;

public interface OrderService {
    Long placeOrder (OrderRequest orderRequest);

    OrderResponse getOrderDetails (Long orderId);
}
