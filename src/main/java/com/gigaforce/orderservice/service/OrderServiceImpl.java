package com.gigaforce.orderservice.service;

import com.gigaforce.orderservice.entity.Order;
import com.gigaforce.orderservice.exception.CustomException;
import com.gigaforce.orderservice.external.client.PaymentService;
import com.gigaforce.orderservice.external.client.ProductService;
import com.gigaforce.orderservice.model.OrderRequest;
import com.gigaforce.orderservice.model.OrderResponse;
import com.gigaforce.orderservice.model.PaymentResponse;
import com.gigaforce.orderservice.model.ProductResponse;
import com.gigaforce.orderservice.repository.OrderRepository;
import com.gigaforce.orderservice.request.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Long placeOrder (OrderRequest orderRequest) {
        /* placing the order **/
        log.info("placing the order request : {}", orderRequest);

        productService.reduceQuantity(orderRequest.getProductId(), orderRequest.getQuantity());

        log.info("Creating order with status : CREATED");

        Order order = Order.builder().amount(orderRequest.getTotalAmount()).orderStatus("CREATED")
                .productId(orderRequest.getProductId()).orderDate(Instant.now()).quantity(orderRequest.getQuantity())
                .build();
        order = orderRepository.save(order);

        log.info("Calling the payment service to complete the payment...");
        PaymentRequest paymentRequest =
                PaymentRequest.builder().orderId(order.getId()).paymentMode(orderRequest.getPaymentMode())
                        .amount(orderRequest.getTotalAmount()).build();
        String orderStatus = null;
        try {
            paymentService.doPayment(paymentRequest);
            log.info("Payment done successfully. Changing the order status as PLACED");
            orderStatus = "PLACED";
        } catch (Exception e) {
            log.info("Error occurred in payment. Changing the status as PAYMENT_FAILED");
            orderStatus = "PAYMENT_FAILED";
        }
        order.setOrderStatus(orderStatus);
        orderRepository.save(order);

        log.info("Order placed successfully with orderId : {}", order.getId());
        return order.getId();
    }

    @Override
    public OrderResponse getOrderDetails (Long orderId) {
        log.info("Get order details by orderId : {}", orderId);
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new CustomException("Order not found for the orderId : " + orderId, "NOT_FOUND", 404));

        log.info("Invoking the product service to fetch the product for id : {}", order.getProductId());

        ProductResponse productResponse =
                restTemplate.getForObject("http://PRODUCT-SERVICE/product/" + order.getProductId(),
                                          ProductResponse.class);

        log.info("Getting the payment details from payment service");
        PaymentResponse paymentResponse =
                restTemplate.getForObject("http://PAYMENT-SERVICE/payment/order/" + order.getId(),
                                          PaymentResponse.class);

        OrderResponse.PaymentDetails paymentDetails =
                OrderResponse.PaymentDetails.builder().paymentId(paymentResponse.getPaymentId())
                        .paymentDate(paymentResponse.getPaymentDate()).orderId(paymentResponse.getOrderId())
                        .paymentStatus(paymentResponse.getStatus()).amount(paymentResponse.getAmount())
                        .paymentMode(paymentResponse.getPaymentMode()).build();

        OrderResponse.ProductDetails productDetails =
                OrderResponse.ProductDetails.builder().productName(productResponse.getProductName())
                        .productId(productResponse.getProductId()).price(productResponse.getPrice()).build();


        return OrderResponse.builder().orderId(order.getId()).amount(order.getAmount()).orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus()).productDetails(productDetails).paymentDetails(paymentDetails)
                .build();
    }
}
