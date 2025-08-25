package com.example.orderservice.service;

import com.example.shared.dto.OrderRequest;
import com.example.shared.dto.OrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceSimpleTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    void testOrderRequestSerialization() throws Exception {
        // Arrange
        OrderRequest orderRequest = new OrderRequest(
            "Test Customer",
            Arrays.asList(
                new OrderItem("P1001", 2, "standard"),
                new OrderItem("P1003", 1, "digital")
            ),
            Instant.now()
        );

        // Act
        String json = objectMapper.writeValueAsString(orderRequest);
        OrderRequest deserialized = objectMapper.readValue(json, OrderRequest.class);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("Test Customer"));
        assertTrue(json.contains("P1001"));
        assertTrue(json.contains("P1003"));
        assertEquals(orderRequest.getCustomerName(), deserialized.getCustomerName());
        assertEquals(orderRequest.getItems().size(), deserialized.getItems().size());
    }

    @Test
    void testOrderItemValidation() {
        // Arrange & Act
        OrderItem validItem = new OrderItem("P1001", 2, "standard");
        OrderItem zeroQuantityItem = new OrderItem("P1001", 0, "standard");
        OrderItem negativeQuantityItem = new OrderItem("P1001", -1, "standard");

        // Assert
        assertNotNull(validItem.getProductId());
        assertTrue(validItem.getQuantity() > 0);
        assertNotNull(validItem.getCategory());
        
        // These should not throw exceptions but represent invalid business logic
        assertEquals(0, zeroQuantityItem.getQuantity());
        assertEquals(-1, negativeQuantityItem.getQuantity());
    }

    @Test
    void testOrderRequestValidation() {
        // Arrange & Act
        OrderRequest validRequest = new OrderRequest(
            "Test Customer",
            Arrays.asList(new OrderItem("P1001", 2, "standard")),
            Instant.now()
        );

        OrderRequest emptyItemsRequest = new OrderRequest(
            "Test Customer",
            Arrays.asList(),
            Instant.now()
        );

        // Assert
        assertNotNull(validRequest.getCustomerName());
        assertFalse(validRequest.getCustomerName().isEmpty());
        assertNotNull(validRequest.getItems());
        assertFalse(validRequest.getItems().isEmpty());
        assertNotNull(validRequest.getRequestedAt());
        
        // Empty items list should be valid from a data structure perspective
        assertTrue(emptyItemsRequest.getItems().isEmpty());
    }
}
