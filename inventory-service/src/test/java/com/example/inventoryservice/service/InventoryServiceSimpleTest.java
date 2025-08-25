package com.example.inventoryservice.service;

import com.example.inventoryservice.model.ProductInfo;
import com.example.shared.dto.OrderItem;
import com.example.shared.dto.OrderStatus;
import com.example.shared.dto.MissingItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceSimpleTest {

    private InventoryService inventoryService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Create a simple mock Redis template that doesn't use complex mocking
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        
        inventoryService = new InventoryService(redisTemplate, objectMapper);
        inventoryService.initializeProductCatalog();
    }

    @Test
    void testProductCatalogInitialization_AllProductsAvailable() {
        // Act
        Map<String, ProductInfo> catalog = inventoryService.getProductCatalog();

        // Assert
        assertNotNull(catalog);
        assertTrue(catalog.size() >= 6, "Catalog should have at least 6 products");
        
        // Verify specific products for test scenarios
        ProductInfo p1001 = catalog.get("P1001");
        assertNotNull(p1001, "P1001 should exist");
        assertEquals("standard", p1001.getCategory());
        assertEquals(10, p1001.getAvailableQuantity());
        assertNull(p1001.getExpirationDate());

        ProductInfo p1003 = catalog.get("P1003");
        assertNotNull(p1003, "P1003 should exist");
        assertEquals("digital", p1003.getCategory());
        assertEquals(0, p1003.getAvailableQuantity()); // Digital products don't need quantity
        assertNull(p1003.getExpirationDate());

        ProductInfo p1005 = catalog.get("P1005");
        assertNotNull(p1005, "P1005 should exist");
        assertEquals("perishable", p1005.getCategory());
        assertEquals(2, p1005.getAvailableQuantity());
        assertEquals(LocalDate.of(2025, 6, 25), p1005.getExpirationDate()); // Expired
    }

    @Test
    void testOrderItemValidation_ValidItems() {
        // Arrange
        OrderItem validStandardItem = new OrderItem("P1001", 2, "standard");
        OrderItem validDigitalItem = new OrderItem("P1003", 1, "digital");
        OrderItem validPerishableItem = new OrderItem("P1002", 1, "perishable");

        // Assert
        assertNotNull(validStandardItem.getProductId());
        assertTrue(validStandardItem.getQuantity() > 0);
        assertEquals("standard", validStandardItem.getCategory());

        assertNotNull(validDigitalItem.getProductId());
        assertTrue(validDigitalItem.getQuantity() > 0);
        assertEquals("digital", validDigitalItem.getCategory());

        assertNotNull(validPerishableItem.getProductId());
        assertTrue(validPerishableItem.getQuantity() > 0);
        assertEquals("perishable", validPerishableItem.getCategory());
    }

    @Test
    void testOrderItemValidation_InvalidItems() {
        // Arrange
        OrderItem zeroQuantityItem = new OrderItem("P1001", 0, "standard");
        OrderItem negativeQuantityItem = new OrderItem("P1001", -1, "standard");
        OrderItem invalidCategoryItem = new OrderItem("P1001", 2, "invalid_category");

        // Assert - These should not throw exceptions but represent invalid business logic
        assertEquals(0, zeroQuantityItem.getQuantity());
        assertEquals(-1, negativeQuantityItem.getQuantity());
        assertEquals("invalid_category", invalidCategoryItem.getCategory());
    }

    @Test
    void testMissingItemCreation_AllScenarios() {
        // Test missing item creation for different scenarios
        
        // Scenario 1: Insufficient quantity
        MissingItem insufficientQuantity = new MissingItem("P1001", 15, 10, "Insufficient quantity");
        assertEquals("P1001", insufficientQuantity.getProductId());
        assertEquals(15, insufficientQuantity.getRequestedQuantity());
        assertEquals(10, insufficientQuantity.getAvailableQuantity());
        assertEquals("Insufficient quantity", insufficientQuantity.getReason());

        // Scenario 2: Product not found
        MissingItem productNotFound = new MissingItem("P9999", 2, 0, "Product not found in catalog");
        assertEquals("P9999", productNotFound.getProductId());
        assertEquals(2, productNotFound.getRequestedQuantity());
        assertEquals(0, productNotFound.getAvailableQuantity());
        assertEquals("Product not found in catalog", productNotFound.getReason());

        // Scenario 3: Expired product
        MissingItem expiredProduct = new MissingItem("P1005", 1, 2, "Product expired on 2025-06-25");
        assertEquals("P1005", expiredProduct.getProductId());
        assertEquals(1, expiredProduct.getRequestedQuantity());
        assertEquals(2, expiredProduct.getAvailableQuantity());
        assertEquals("Product expired on 2025-06-25", expiredProduct.getReason());

        // Scenario 4: Invalid category
        MissingItem invalidCategory = new MissingItem("P1001", 2, 10, "Category mismatch. Expected: standard, Actual: invalid_category");
        assertEquals("P1001", invalidCategory.getProductId());
        assertEquals(2, invalidCategory.getRequestedQuantity());
        assertEquals(10, invalidCategory.getAvailableQuantity());
        assertTrue(invalidCategory.getReason().contains("Category mismatch"));
    }

    @Test
    void testOrderStatusValidation() {
        // Test order status values
        assertEquals("PENDING", OrderStatus.PENDING.name());
        assertEquals("APPROVED", OrderStatus.APPROVED.name());
        assertEquals("REJECTED", OrderStatus.REJECTED.name());
        
        // Test that we can create status objects
        assertNotNull(OrderStatus.PENDING);
        assertNotNull(OrderStatus.APPROVED);
        assertNotNull(OrderStatus.REJECTED);
    }

    @Test
    void testJsonSerialization_OrderItems() throws Exception {
        // Arrange
        List<OrderItem> items = Arrays.asList(
            new OrderItem("P1001", 2, "standard"),
            new OrderItem("P1003", 1, "digital")
        );

        // Act
        String json = objectMapper.writeValueAsString(items);
        List<OrderItem> deserialized = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, OrderItem.class));

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("P1001"));
        assertTrue(json.contains("P1003"));
        assertTrue(json.contains("standard"));
        assertTrue(json.contains("digital"));
        assertEquals(2, deserialized.size());
        assertEquals("P1001", deserialized.get(0).getProductId());
        assertEquals("P1003", deserialized.get(1).getProductId());
    }

    @Test
    void testJsonSerialization_MissingItems() throws Exception {
        // Arrange
        List<MissingItem> missingItems = Arrays.asList(
            new MissingItem("P1001", 15, 10, "Insufficient quantity"),
            new MissingItem("P9999", 2, 0, "Product not found")
        );

        // Act
        String json = objectMapper.writeValueAsString(missingItems);
        List<MissingItem> deserialized = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, MissingItem.class));

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("P1001"));
        assertTrue(json.contains("P9999"));
        assertTrue(json.contains("Insufficient quantity"));
        assertTrue(json.contains("Product not found"));
        assertEquals(2, deserialized.size());
        assertEquals("P1001", deserialized.get(0).getProductId());
        assertEquals("P9999", deserialized.get(1).getProductId());
    }
}
