package com.example.notificationservice.service;

import com.example.shared.dto.Order;
import com.example.shared.dto.OrderItem;
import com.example.shared.dto.OrderStatus;
import com.example.shared.dto.MissingItem;
import com.example.shared.event.InventoryCheckResultEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceSimpleTest {

    private NotificationService notificationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        
        notificationService = new NotificationService(redisTemplate, objectMapper);
    }

    @Test
    void testOrderCreation_ValidOrder() {
        // Arrange
        OrderItem item1 = new OrderItem("P1001", 2, "standard");
        OrderItem item2 = new OrderItem("P1003", 1, "digital");
        
        Order order = new Order(
            "ORD-TEST123",
            "Test Customer",
            Arrays.asList(item1, item2),
            Instant.now(),
            OrderStatus.PENDING,
            Instant.now()
        );

        // Assert
        assertNotNull(order);
        assertEquals("ORD-TEST123", order.getOrderId());
        assertEquals("Test Customer", order.getCustomerName());
        assertEquals(2, order.getItems().size());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertNotNull(order.getRequestedAt());
    }

    @Test
    void testInventoryCheckResultEvent_ApprovedOrder() {
        // Arrange
        InventoryCheckResultEvent event = new InventoryCheckResultEvent("ORD-TEST123", OrderStatus.APPROVED);

        // Assert
        assertNotNull(event);
        assertEquals("ORD-TEST123", event.getOrderId());
        assertEquals(OrderStatus.APPROVED, event.getStatus());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void testInventoryCheckResultEvent_RejectedOrder() {
        // Arrange
        InventoryCheckResultEvent event = new InventoryCheckResultEvent("ORD-TEST123", OrderStatus.REJECTED);

        // Assert
        assertNotNull(event);
        assertEquals("ORD-TEST123", event.getOrderId());
        assertEquals(OrderStatus.REJECTED, event.getStatus());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void testMissingItemCreation_AllScenarios() {
        // Test missing item creation for different scenarios
        
        // Scenario 1: Perishable item expired
        MissingItem expiredItem = new MissingItem("P1005", 1, 2, "Product expired on 2025-06-25");
        assertEquals("P1005", expiredItem.getProductId());
        assertEquals(1, expiredItem.getRequestedQuantity());
        assertEquals(2, expiredItem.getAvailableQuantity());
        assertTrue(expiredItem.getReason().contains("expired"));

        // Scenario 2: Insufficient quantity
        MissingItem insufficientItem = new MissingItem("P1001", 15, 10, "Insufficient quantity");
        assertEquals("P1001", insufficientItem.getProductId());
        assertEquals(15, insufficientItem.getRequestedQuantity());
        assertEquals(10, insufficientItem.getAvailableQuantity());
        assertEquals("Insufficient quantity", insufficientItem.getReason());

        // Scenario 3: Product not found
        MissingItem notFoundItem = new MissingItem("P9999", 2, 0, "Product not found in catalog");
        assertEquals("P9999", notFoundItem.getProductId());
        assertEquals(2, notFoundItem.getRequestedQuantity());
        assertEquals(0, notFoundItem.getAvailableQuantity());
        assertTrue(notFoundItem.getReason().contains("not found"));

        // Scenario 4: Invalid category
        MissingItem invalidCategoryItem = new MissingItem("P1001", 2, 10, "Category mismatch. Expected: standard, Actual: invalid_category");
        assertEquals("P1001", invalidCategoryItem.getProductId());
        assertEquals(2, invalidCategoryItem.getRequestedQuantity());
        assertEquals(10, invalidCategoryItem.getAvailableQuantity());
        assertTrue(invalidCategoryItem.getReason().contains("Category mismatch"));
    }

    @Test
    void testJsonSerialization_Order() throws Exception {
        // Arrange
        Order order = new Order(
            "ORD-TEST123",
            "Test Customer",
            Arrays.asList(new OrderItem("P1001", 2, "standard")),
            Instant.now(),
            OrderStatus.APPROVED,
            Instant.now()
        );

        // Act
        String json = objectMapper.writeValueAsString(order);
        Order deserialized = objectMapper.readValue(json, Order.class);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("ORD-TEST123"));
        assertTrue(json.contains("Test Customer"));
        assertTrue(json.contains("P1001"));
        assertTrue(json.contains("APPROVED"));
        assertEquals("ORD-TEST123", deserialized.getOrderId());
        assertEquals("Test Customer", deserialized.getCustomerName());
        assertEquals(OrderStatus.APPROVED, deserialized.getStatus());
    }

    @Test
    void testJsonSerialization_MissingItems() throws Exception {
        // Arrange
        List<MissingItem> missingItems = Arrays.asList(
            new MissingItem("P1005", 1, 2, "Product expired on 2025-06-25"),
            new MissingItem("P1001", 15, 10, "Insufficient quantity")
        );

        // Act
        String json = objectMapper.writeValueAsString(missingItems);
        List<MissingItem> deserialized = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, MissingItem.class));

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("P1005"));
        assertTrue(json.contains("P1001"));
        assertTrue(json.contains("expired"));
        assertTrue(json.contains("Insufficient quantity"));
        assertEquals(2, deserialized.size());
        assertEquals("P1005", deserialized.get(0).getProductId());
        assertEquals("P1001", deserialized.get(1).getProductId());
    }

    @Test
    void testOrderStatusValidation() {
        // Test all order status values
        assertEquals("PENDING", OrderStatus.PENDING.name());
        assertEquals("APPROVED", OrderStatus.APPROVED.name());
        assertEquals("REJECTED", OrderStatus.REJECTED.name());
        
        // Test that we can create status objects
        assertNotNull(OrderStatus.PENDING);
        assertNotNull(OrderStatus.APPROVED);
        assertNotNull(OrderStatus.REJECTED);
    }

    @Test
    void testOrderItemValidation() {
        // Test order item creation and validation
        OrderItem standardItem = new OrderItem("P1001", 2, "standard");
        OrderItem digitalItem = new OrderItem("P1003", 1, "digital");
        OrderItem perishableItem = new OrderItem("P1002", 1, "perishable");

        // Assert
        assertNotNull(standardItem.getProductId());
        assertTrue(standardItem.getQuantity() > 0);
        assertEquals("standard", standardItem.getCategory());

        assertNotNull(digitalItem.getProductId());
        assertTrue(digitalItem.getQuantity() > 0);
        assertEquals("digital", digitalItem.getCategory());

        assertNotNull(perishableItem.getProductId());
        assertTrue(perishableItem.getQuantity() > 0);
        assertEquals("perishable", perishableItem.getCategory());
    }

    @Test
    void testOrderItemValidation_EdgeCases() {
        // Test edge cases for order items
        OrderItem zeroQuantityItem = new OrderItem("P1001", 0, "standard");
        OrderItem negativeQuantityItem = new OrderItem("P1001", -1, "standard");
        OrderItem invalidCategoryItem = new OrderItem("P1001", 2, "invalid_category");

        // Assert - These should not throw exceptions but represent invalid business logic
        assertEquals(0, zeroQuantityItem.getQuantity());
        assertEquals(-1, negativeQuantityItem.getQuantity());
        assertEquals("invalid_category", invalidCategoryItem.getCategory());
    }
}
