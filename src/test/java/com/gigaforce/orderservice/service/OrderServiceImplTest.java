package com.gigaforce.orderservice.service;

import com.gigaforce.orderservice.entity.Order;
import com.gigaforce.orderservice.enums.PaymentMode;
import com.gigaforce.orderservice.exception.CustomException;
import com.gigaforce.orderservice.external.client.PaymentService;
import com.gigaforce.orderservice.external.client.ProductService;
import com.gigaforce.orderservice.model.OrderRequest;
import com.gigaforce.orderservice.model.OrderResponse;
import com.gigaforce.orderservice.model.PaymentResponse;
import com.gigaforce.orderservice.model.ProductResponse;
import com.gigaforce.orderservice.repository.OrderRepository;
import com.gigaforce.orderservice.request.PaymentRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    OrderService orderService = new OrderServiceImpl();

    @DisplayName("Get Order - Success Scenario")
    @Test
    void test_When_Order_Success () {

        /*
         * Mocking the method which is calling by this method
         */
        Order order = getMockOrder();
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));
        when(restTemplate.getForObject("http://PRODUCT-SERVICE/product/" + order.getProductId(),
                                               ProductResponse.class)).thenReturn(getMockProductResponse());
        when(restTemplate.getForObject("http://PAYMENT-SERVICE/payment/order/" + order.getId(),
                                               PaymentResponse.class)).thenReturn(getMockPaymentResponse());
        /* actual method calling **/
        OrderResponse orderResponse = orderService.getOrderDetails(1L);

        /*
         * verify
         */
        verify(orderRepository, times(1)).findById(1L);
        verify(restTemplate, times(1))
                .getForObject("http://PRODUCT-SERVICE/product/" + +order.getProductId(), ProductResponse.class);
        verify(restTemplate, times(1))
                .getForObject("http://PAYMENT-SERVICE/payment/order/" + order.getId(), PaymentResponse.class);

        /*
         * assertion
         */
        Assertions.assertNotNull(orderResponse);
        Assertions.assertEquals(order.getId(), orderResponse.getOrderId());
    }

    @DisplayName("Get Orders - Failure Scenario")
    @Test
    void test_When_Get_Order_Not_Found_then_Not_Found () {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.ofNullable(null));
        CustomException exception =
                Assertions.assertThrows(CustomException.class, () -> orderService.getOrderDetails(1L));
        Assertions.assertEquals("NOT_FOUND", exception.getErrorCode());
        Assertions.assertEquals(404, exception.getStatus());
        verify(orderRepository, times(1)).findById(anyLong());
    }

    @DisplayName("Place Order - Success Scenario")
    @Test
    void test_When_Place_Order_Success () {
        Order order = getMockOrder();
        OrderRequest orderRequest = getMockOrderRequest();

        when(orderRepository.save(any(Order.class))).thenReturn(order);
        doNothing().when(productService).reduceQuantity(anyLong(), anyLong());
        when(paymentService.doPayment(any(PaymentRequest.class)))
                .thenReturn(new ResponseEntity<Long>(1L, HttpStatus.OK));

        Long orderId = orderService.placeOrder(orderRequest);

        verify(orderRepository, times(2))
                .save(any());
        verify(productService, times(1))
                .reduceQuantity(anyLong(),anyLong());
        verify(paymentService, times(1))
                .doPayment(any(PaymentRequest.class));
        Assertions.assertEquals(order.getId(), orderId);

    }

    @DisplayName("Place Order - Payment failed Scenario")
    @Test
    void test_When_Place_Order_Payment_Fails_then_Order_Placed(){

        Order order = getMockOrder();
        OrderRequest orderRequest = getMockOrderRequest();

        when(orderRepository.save(any(Order.class))).thenReturn(order);
        doNothing().when(productService).reduceQuantity(anyLong(), anyLong());

        when(paymentService.doPayment(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException());

        Long orderId = orderService.placeOrder(orderRequest);

        verify(orderRepository, times(2))
                .save(any());
        verify(productService, times(1))
                .reduceQuantity(anyLong(),anyLong());

        verify(paymentService, times(1))
                .doPayment(any(PaymentRequest.class));

        Assertions.assertEquals(order.getId(), orderId);



    }

    private OrderRequest getMockOrderRequest () {
        return OrderRequest.builder().productId(1L).quantity(100L).paymentMode(PaymentMode.CASH).totalAmount(100L)
                .build();
    }

    private PaymentResponse getMockPaymentResponse () {
        return PaymentResponse.builder().paymentId(1L).paymentDate(Instant.now()).amount(200L)
                .paymentMode(PaymentMode.CASH).orderId(1L).status("ACCEPTED").build();
    }

    private ProductResponse getMockProductResponse () {
        return ProductResponse.builder().productId(2L).productName("iPhone").price(100L).quantity(200L).build();
    }

    private Order getMockOrder () {
        return Order.builder().orderStatus("PLACED").orderDate(Instant.now()).id(1L).amount(100L).quantity(200L)
                .productId(2L).build();
    }

}