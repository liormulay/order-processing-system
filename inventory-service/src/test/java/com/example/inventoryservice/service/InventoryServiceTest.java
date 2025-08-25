package com.example.inventoryservice.service;

import com.example.inventoryservice.model.ProductInfo;
import com.example.shared.dto.OrderItem;
import com.example.shared.dto.OrderStatus;
import com.example.shared.dto.MissingItem;
import com.example.shared.event.InventoryCheckResultEvent;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventoryServiceTest {

    private InventoryService inventoryService;
    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        inventoryService = new InventoryService(redisTemplate, objectMapper);
        inventoryService.initializeProductCatalog();
    }

    @Test
    void testCheckInventory_AllProductsAvailable_OrderApproved() throws Exception {
        // Arrange
        List<OrderItem> items = Arrays.asList(
            new OrderItem("P1001", 2, "standard"),  // Available: 10
            new OrderItem("P1003", 1, "digital")    // Digital: always available
        );

        // Mock Redis responses
        String orderJson = createOrderJson("ORD-TEST123", items);
        when(valueOperations.get("order:ORD-TEST123")).thenReturn(orderJson);

        // Act
        InventoryCheckResultEvent result = inventoryService.checkInventory("ORD-TEST123", items);

        // Assert
        assertNotNull(result);
        assertEquals("ORD-TEST123", result.getOrderId());
        assertEquals(OrderStatus.APPROVED, result.getStatus());
        
        // Verify Redis was called to update order status
        verify(valueOperations, times(2)).set(anyString(), anyString(), any());
        verify(valueOperations, never()).set(eq("missingItems:ORD-TEST123"), anyString(), any());
    }

    @Test
    void testCheckInventory_SomeProductsUnavailable_OrderRejected() throws Exception {
        // Arrange
        List<OrderItem> items = Arrays.asList(
            new OrderItem("P1001", 15, "standard"),  // Available: 10, insufficient
            new OrderItem("P1003", 1, "digital")     // Digital: always available
        );

        // Mock Redis responses
        String orderJson = createOrderJson("ORD-TEST123", items);
        when(valueOperations.get("order:ORD-TEST123")).thenReturn(orderJson);

        // Act
        InventoryCheckResultEvent result = inventoryService.checkInventory("ORD-TEST123", items);

        // Assert
        assertNotNull(result);
        assertEquals("ORD-TEST123", result.getOrderId());
        assertEquals(OrderStatus.REJECTED, result.getStatus());
        
        // Verify missing items were stored in Redis
        verify(valueOperations).set(eq("missingItems:ORD-TEST123"), anyString(), any());
        verify(valueOperations, times(2)).set(anyString(), anyString(), any());
    }

    @Test
    void testCheckInventory_PerishableProductExpired_OrderRejected() throws Exception {
        // Arrange
        List<OrderItem> items = Arrays.asList(
            new OrderItem("P1005", 1, "perishable")  // Expired product
        );

        // Mock Redis responses
        String orderJson = createOrderJson("ORD-TEST123", items);
        when(valueOperations.get("order:ORD-TEST123")).thenReturn(orderJson);

        // Act
        InventoryCheckResultEvent result = inventoryService.checkInventory("ORD-TEST123", items);

        // Assert
        assertNotNull(result);
        assertEquals("ORD-TEST123", result.getOrderId());
        assertEquals(OrderStatus.REJECTED, result.getStatus());
        
        // Verify missing items were stored with expiration reason
        verify(valueOperations).set(eq("missingItems:ORD-TEST123"), anyString(), any());
    }

    @Test
    void testCheckInventory_InvalidCategory_OrderRejected() throws Exception {
        // Arrange
        List<OrderItem> items = Arrays.asList(
            new OrderItem("P1001", 2, "invalid_category")  // Invalid category
        );

        // Mock Redis responses
        String orderJson = createOrderJson("ORD-TEST123", items);
        when(valueOperations.get("order:ORD-TEST123")).thenReturn(orderJson);

        // Act
        InventoryCheckResultEvent result = inventoryService.checkInventory("ORD-TEST123", items);

        // Assert
        assertNotNull(result);
        assertEquals("ORD-TEST123", result.getOrderId());
        assertEquals(OrderStatus.REJECTED, result.getStatus());
        
        // Verify missing items were stored with category mismatch reason
        verify(valueOperations).set(eq("missingItems:ORD-TEST123"), anyString(), any());
    }

    @Test
    void testCheckInventory_ProductNotFound_OrderRejected() throws Exception {
        // Arrange
        List<OrderItem> items = Arrays.asList(
            new OrderItem("P9999", 2, "standard")  // Non-existent product
        );

        // Mock Redis responses
        String orderJson = createOrderJson("ORD-TEST123", items);
        when(valueOperations.get("order:ORD-TEST123")).thenReturn(orderJson);

        // Act
        InventoryCheckResultEvent result = inventoryService.checkInventory("ORD-TEST123", items);

        // Assert
        assertNotNull(result);
        assertEquals("ORD-TEST123", result.getOrderId());
        assertEquals(OrderStatus.REJECTED, result.getStatus());
        
        // Verify missing items were stored with product not found reason
        verify(valueOperations).set(eq("missingItems:ORD-TEST123"), anyString(), any());
    }

    @Test
    void testCheckInventory_RedisInaccessible_GracefulError() throws Exception {
        // Arrange
        List<OrderItem> items = Arrays.asList(
            new OrderItem("P1001", 2, "standard")
        );

        // Mock Redis failure
        when(valueOperations.get("order:ORD-TEST123")).thenThrow(new RuntimeException("Redis connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            inventoryService.checkInventory("ORD-TEST123", items);
        });
    }

    @Test
    void testProductCatalogInitialization() {
        // Act
        Map<String, ProductInfo> catalog = inventoryService.getProductCatalog();

        // Assert
        assertNotNull(catalog);
        assertTrue(catalog.size() >= 6); // Should have at least 6 products
        
        // Verify specific products
        ProductInfo p1001 = catalog.get("P1001");
        assertNotNull(p1001);
        assertEquals("standard", p1001.getCategory());
        assertEquals(10, p1001.getAvailableQuantity());
        assertNull(p1001.getExpirationDate());

        ProductInfo p1002 = catalog.get("P1002");
        assertNotNull(p1002);
        assertEquals("perishable", p1002.getCategory());
        assertEquals(3, p1002.getAvailableQuantity());
        assertEquals(LocalDate.of(2025, 7, 1), p1002.getExpirationDate());

        ProductInfo p1003 = catalog.get("P1003");
        assertNotNull(p1003);
        assertEquals("digital", p1003.getCategory());
        assertEquals(0, p1003.getAvailableQuantity()); // Digital products don't need quantity
        assertNull(p1003.getExpirationDate());

        ProductInfo p1005 = catalog.get("P1005");
        assertNotNull(p1005);
        assertEquals("perishable", p1005.getCategory());
        assertEquals(2, p1005.getAvailableQuantity());
        assertEquals(LocalDate.of(2025, 6, 25), p1005.getExpirationDate()); // Expired
    }

    private String createOrderJson(String orderId, List<OrderItem> items) throws Exception {
        return String.format(
            "{\"orderId\":\"%s\",\"customerName\":\"Test Customer\",\"items\":%s,\"requestedAt\":\"2025-01-01T10:00:00Z\",\"status\":\"PENDING\"}",
            orderId,
            objectMapper.writeValueAsString(items)
        );
    }
}
