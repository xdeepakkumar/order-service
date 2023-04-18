package com.gigaforce.orderservice.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gigaforce.orderservice.OrderServiceConfig;
import com.gigaforce.orderservice.entity.Order;
import com.gigaforce.orderservice.enums.PaymentMode;
import com.gigaforce.orderservice.model.OrderRequest;
import com.gigaforce.orderservice.model.OrderResponse;
import com.gigaforce.orderservice.repository.OrderRepository;
import com.gigaforce.orderservice.service.OrderService;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.util.StreamUtils.copyToString;

@SpringBootTest({"server.port=0"})
@EnableConfigurationProperties
@AutoConfigureMockMvc
@ContextConfiguration(classes = {OrderServiceConfig.class})
class OrderControllerTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MockMvc mockMvc;

    static WireMockExtension wireMockServer =
            WireMockExtension.newInstance().options(WireMockConfiguration.wireMockConfig().port(8080)).build();

    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @BeforeEach
    void setup () throws IOException {
        getProductDetailsResponse();
        doPayment();
        getPaymentDetails();
        reduceQuantity();
    }

    private void reduceQuantity () {
        wireMockServer.stubFor(put(urlMatching("/product/reduceQuantity/.*")).willReturn(
                aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
    }

    private void getPaymentDetails () throws IOException {
        wireMockServer.stubFor(get(urlMatching("/payment/.*")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                                                                                  .withHeader("Content-Type",
                                                                                              MediaType.APPLICATION_JSON_VALUE)
                                                                                  .withBody(copyToString(
                                                                                          OrderControllerTest.class.getClassLoader()
                                                                                                  .getResourceAsStream(
                                                                                                          "/mock/GetPayment.json"),
                                                                                          Charset.defaultCharset()))));
    }

    private void doPayment () {
        wireMockServer.stubFor(post(urlEqualTo("/payment")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                                                                               .withHeader("Content-Type",
                                                                                           MediaType.APPLICATION_JSON_VALUE)));
    }

    private void getProductDetailsResponse () throws IOException {
        /*
         * GET /product/1
         */
        wireMockServer.stubFor(get("/product/1").willReturn(aResponse().withStatus(HttpStatus.OK.value())
                                                                    .withHeader("Content-Type",
                                                                                MediaType.APPLICATION_JSON_VALUE)
                                                                    .withBody(copyToString(
                                                                            OrderControllerTest.class.getClassLoader()
                                                                                    .getResourceAsStream(
                                                                                            "/mock/GetProduct.json"),
                                                                            Charset.defaultCharset()))));
    }

    @Test
    void test_WhenPlaceOrder_DoPayment_Success () throws Exception {
        OrderRequest orderRequest = getMockOrderRequest();
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/order/placeOrder")
                                                      .with(jwt().authorities(new SimpleGrantedAuthority("Customer")))
                                                      .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                      .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String orderId = mvcResult.getResponse().getContentAsString();
        Optional<Order> order = orderRepository.findById(Long.valueOf(orderId));
        assertTrue(order.isPresent());
        Order o = order.get();
        assertEquals(Long.parseLong(orderId), o.getId());
        assertEquals("PLACED", o.getOrderStatus());
        assertEquals(orderRequest.getTotalAmount(), o.getAmount());
        assertEquals(orderRequest.getQuantity(), o.getQuantity());
    }

    @Test
    void test_WhenPlaceOrderWithWrongAccess_thenThrow403 () throws Exception {
        OrderRequest orderRequest = getMockOrderRequest();
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/order/placeOrder")
                                                      .with(jwt().authorities(new SimpleGrantedAuthority("Admin")))
                                                      .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                      .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();
    }

    @Test
    void test_whenGetOrder_Success () throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.get("/order/1").with(jwt().authorities(new SimpleGrantedAuthority("ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        String actualResponse = mvcResult.getResponse().getContentAsString();
        Order order = orderRepository.findById(1L).get();
        String expectedResponse = getOrderResponse(order);
        assertEquals(expectedResponse,actualResponse);

    }

    private String getOrderResponse (Order order) throws IOException {
        OrderResponse.PaymentDetails paymentDetails = objectMapper.readValue(
                copyToString(OrderControllerTest.class.getClassLoader().getResourceAsStream("/mock/GetOrder.json"),
                             Charset.defaultCharset()), OrderResponse.PaymentDetails.class);
        paymentDetails.setPaymentStatus("SUCCESS");

        OrderResponse.ProductDetails productDetails = objectMapper.readValue(
                copyToString(OrderResponse.ProductDetails.class.getClassLoader().getResourceAsStream(
                        "/mock/GetProduct.json"
                ), Charset.defaultCharset()), OrderResponse.ProductDetails.class);

        OrderResponse orderResponse = OrderResponse.builder()
                .paymentDetails(paymentDetails)
                .productDetails(productDetails)
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .amount(order.getAmount())
                .orderId(order.getId())
                .build();
        return objectMapper.writeValueAsString(orderResponse);
    }

    private OrderRequest getMockOrderRequest () {
        return OrderRequest.builder().productId(1L).paymentMode(PaymentMode.CASH).quantity(10L).totalAmount(200L)
                .build();
    }
}