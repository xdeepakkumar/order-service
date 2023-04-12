package com.gigaforce.orderservice.service;

import com.gigaforce.orderservice.entity.Order;
import com.gigaforce.orderservice.model.OrderRequest;
import com.gigaforce.orderservice.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService{

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public Long placeOrder (OrderRequest orderRequest) {
        /* placing the order **/
        log.info("placing the order request : {}", orderRequest);
        Order order = Order.builder()
                .amount(orderRequest.getTotalAmount())
                .orderStatus("CREATED")
                .productId(orderRequest.getProductId())
                .orderDate(Instant.now())
                .quantity(orderRequest.getQuantity())
                .build();
        order = orderRepository.save(order);
        log.info("Order placed successfully with orderId : {}", order.getId());
        return order.getId();
    }
}
