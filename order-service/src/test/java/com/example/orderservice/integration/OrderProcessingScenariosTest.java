package com.example.orderservice.integration;

import com.example.shared.dto.OrderRequest;
import com.example.shared.dto.OrderItem;
import com.example.shared.dto.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderProcessingScenariosTest {

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        restTemplate = new TestRestTemplate();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        baseUrl = "http://localhost:" + port;
    }

    @Test
    void testScenario1_AllProductsAvailable_OrderApproved() throws Exception {
        // Scenario 1: All products available -> Order approved, success log
        
        // Arrange
        OrderRequest orderRequest = new OrderRequest(
            "Test Customer - All Available",
            Arrays.asList(
                new OrderItem("P1001", 2, "standard"),  // Available: 10
                new OrderItem("P1003", 1, "digital")    // Digital: always available
            ),
            Instant.now()
        );

        // Act - Create order
        ResponseEntity<String> createResponse = createOrder(orderRequest);
        
        // Assert - Order created successfully
        assertEquals(201, createResponse.getStatusCodeValue());
        assertTrue(createResponse.getBody().contains("PENDING"));
        
        // Extract order ID
        String orderId = extractOrderId(createResponse.getBody());
        assertNotNull(orderId);
        
        // Wait for async processing
        Thread.sleep(3000);
        
        // Act - Check order status
        ResponseEntity<String> statusResponse = getOrderStatus(orderId);
        
        // Assert - Order should be approved
        assertEquals(200, statusResponse.getStatusCodeValue());
        assertTrue(statusResponse.getBody().contains("APPROVED"));
        
        System.out.println("✅ Scenario 1 PASSED: All products available -> Order approved");
    }

    @Test
    void testScenario2_SomeProductsUnavailable_OrderRejected() throws Exception {
        // Scenario 2: Some products unavailable -> Order rejected, missing items logged
        
        // Arrange
        OrderRequest orderRequest = new OrderRequest(
            "Test Customer - Some Unavailable",
            Arrays.asList(
                new OrderItem("P1001", 15, "standard"),  // Available: 10, insufficient
                new OrderItem("P1003", 1, "digital")     // Digital: always available
            ),
            Instant.now()
        );

        // Act - Create order
        ResponseEntity<String> createResponse = createOrder(orderRequest);
        
        // Assert - Order created successfully
        assertEquals(201, createResponse.getStatusCodeValue());
        assertTrue(createResponse.getBody().contains("PENDING"));
        
        // Extract order ID
        String orderId = extractOrderId(createResponse.getBody());
        assertNotNull(orderId);
        
        // Wait for async processing
        Thread.sleep(3000);
        
        // Act - Check order status
        ResponseEntity<String> statusResponse = getOrderStatus(orderId);
        
        // Assert - Order should be rejected
        assertEquals(200, statusResponse.getStatusCodeValue());
        assertTrue(statusResponse.getBody().contains("REJECTED"));
        
        System.out.println("✅ Scenario 2 PASSED: Some products unavailable -> Order rejected");
    }

    @Test
    void testScenario3_PerishableItemExpired_OrderRejected() throws Exception {
        // Scenario 3: Perishable item expired -> Order rejected
        
        // Arrange
        OrderRequest orderRequest = new OrderRequest(
            "Test Customer - Expired Perishable",
            Arrays.asList(
                new OrderItem("P1005", 1, "perishable")  // Expired product
            ),
            Instant.now()
        );

        // Act - Create order
        ResponseEntity<String> createResponse = createOrder(orderRequest);
        
        // Assert - Order created successfully
        assertEquals(201, createResponse.getStatusCodeValue());
        assertTrue(createResponse.getBody().contains("PENDING"));
        
        // Extract order ID
        String orderId = extractOrderId(createResponse.getBody());
        assertNotNull(orderId);
        
        // Wait for async processing
        Thread.sleep(3000);
        
        // Act - Check order status
        ResponseEntity<String> statusResponse = getOrderStatus(orderId);
        
        // Assert - Order should be rejected
        assertEquals(200, statusResponse.getStatusCodeValue());
        assertTrue(statusResponse.getBody().contains("REJECTED"));
        
        System.out.println("✅ Scenario 3 PASSED: Perishable item expired -> Order rejected");
    }

    @Test
    void testScenario4_InvalidCategory_OrderRejected() throws Exception {
        // Scenario 4: Invalid category -> Order rejected or skipped with log
        
        // Arrange
        OrderRequest orderRequest = new OrderRequest(
            "Test Customer - Invalid Category",
            Arrays.asList(
                new OrderItem("P1001", 2, "invalid_category")  // Invalid category
            ),
            Instant.now()
        );

        // Act - Create order
        ResponseEntity<String> createResponse = createOrder(orderRequest);
        
        // Assert - Order created successfully
        assertEquals(201, createResponse.getStatusCodeValue());
        assertTrue(createResponse.getBody().contains("PENDING"));
        
        // Extract order ID
        String orderId = extractOrderId(createResponse.getBody());
        assertNotNull(orderId);
        
        // Wait for async processing
        Thread.sleep(3000);
        
        // Act - Check order status
        ResponseEntity<String> statusResponse = getOrderStatus(orderId);
        
        // Assert - Order should be rejected
        assertEquals(200, statusResponse.getStatusCodeValue());
        assertTrue(statusResponse.getBody().contains("REJECTED"));
        
        System.out.println("✅ Scenario 4 PASSED: Invalid category -> Order rejected");
    }

    @Test
    void testScenario5_ProductNotFound_OrderRejected() throws Exception {
        // Scenario 5: Product not found -> Order rejected
        
        // Arrange
        OrderRequest orderRequest = new OrderRequest(
            "Test Customer - Product Not Found",
            Arrays.asList(
                new OrderItem("P9999", 2, "standard")  // Non-existent product
            ),
            Instant.now()
        );

        // Act - Create order
        ResponseEntity<String> createResponse = createOrder(orderRequest);
        
        // Assert - Order created successfully
        assertEquals(201, createResponse.getStatusCodeValue());
        assertTrue(createResponse.getBody().contains("PENDING"));
        
        // Extract order ID
        String orderId = extractOrderId(createResponse.getBody());
        assertNotNull(orderId);
        
        // Wait for async processing
        Thread.sleep(3000);
        
        // Act - Check order status
        ResponseEntity<String> statusResponse = getOrderStatus(orderId);
        
        // Assert - Order should be rejected
        assertEquals(200, statusResponse.getStatusCodeValue());
        assertTrue(statusResponse.getBody().contains("REJECTED"));
        
        System.out.println("✅ Scenario 5 PASSED: Product not found -> Order rejected");
    }

    @Test
    void testScenario6_MixedValidAndInvalid_OrderRejected() throws Exception {
        // Scenario 6: Mixed valid and invalid items -> Order rejected
        
        // Arrange
        OrderRequest orderRequest = new OrderRequest(
            "Test Customer - Mixed Items",
            Arrays.asList(
                new OrderItem("P1001", 2, "standard"),  // Valid
                new OrderItem("P1005", 1, "perishable"), // Expired
                new OrderItem("P1003", 1, "digital")     // Valid
            ),
            Instant.now()
        );

        // Act - Create order
        ResponseEntity<String> createResponse = createOrder(orderRequest);
        
        // Assert - Order created successfully
        assertEquals(201, createResponse.getStatusCodeValue());
        assertTrue(createResponse.getBody().contains("PENDING"));
        
        // Extract order ID
        String orderId = extractOrderId(createResponse.getBody());
        assertNotNull(orderId);
        
        // Wait for async processing
        Thread.sleep(3000);
        
        // Act - Check order status
        ResponseEntity<String> statusResponse = getOrderStatus(orderId);
        
        // Assert - Order should be rejected due to expired item
        assertEquals(200, statusResponse.getStatusCodeValue());
        assertTrue(statusResponse.getBody().contains("REJECTED"));
        
        System.out.println("✅ Scenario 6 PASSED: Mixed valid and invalid items -> Order rejected");
    }

    private ResponseEntity<String> createOrder(OrderRequest orderRequest) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = objectMapper.writeValueAsString(orderRequest);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        
        return restTemplate.exchange(
            baseUrl + "/orders",
            HttpMethod.POST,
            entity,
            String.class
        );
    }

    private ResponseEntity<String> getOrderStatus(String orderId) {
        return restTemplate.exchange(
            baseUrl + "/orders/" + orderId,
            HttpMethod.GET,
            null,
            String.class
        );
    }

    private String extractOrderId(String responseBody) throws Exception {
        return objectMapper.readTree(responseBody).get("orderId").asText();
    }
}
