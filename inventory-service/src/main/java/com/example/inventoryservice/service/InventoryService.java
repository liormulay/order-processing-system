package com.example.inventoryservice.service;

import com.example.inventoryservice.model.ProductInfo;
import com.example.shared.dto.Order;
import com.example.shared.dto.OrderItem;
import com.example.shared.dto.OrderStatus;
import com.example.shared.dto.MissingItem;
import com.example.shared.event.InventoryCheckResultEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    private static final Duration REDIS_TTL = Duration.ofMinutes(10); // 10 minutes TTL
    
    private final Map<String, ProductInfo> productCatalog = new HashMap<>();
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public InventoryService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void initializeProductCatalog() {
        // Initialize the product catalog with sample data
        productCatalog.put("P1001", new ProductInfo("standard", 10, null));
        productCatalog.put("P1002", new ProductInfo("perishable", 3, LocalDate.of(2025, 7, 1)));
        productCatalog.put("P1003", new ProductInfo("digital", 0, null));
        productCatalog.put("P1004", new ProductInfo("standard", 5, null));
        productCatalog.put("P1005", new ProductInfo("perishable", 2, LocalDate.of(2025, 6, 25))); // Expired
        productCatalog.put("P1006", new ProductInfo("digital", 100, null));
        
        logger.info("Product catalog initialized with {} products", productCatalog.size());
    }
    
    public InventoryCheckResultEvent checkInventory(String orderId, List<OrderItem> items) {
        logger.info("Checking inventory for order: {} with {} items", orderId, items.size());
        
        List<MissingItem> missingItems = new ArrayList<>();
        boolean allItemsAvailable = true;
        
        for (OrderItem item : items) {
            MissingItem missingItem = checkItemAvailability(item);
            if (missingItem != null) {
                missingItems.add(missingItem);
                allItemsAvailable = false;
            }
        }
        
        OrderStatus status = allItemsAvailable ? OrderStatus.APPROVED : OrderStatus.REJECTED;
        
        // Store missing items in Redis if any
        if (!missingItems.isEmpty()) {
            storeMissingItemsInRedis(orderId, missingItems);
        }
        
        // Update order status directly in Redis
        updateOrderStatusInRedis(orderId, status);
        
        InventoryCheckResultEvent result = new InventoryCheckResultEvent(orderId, status);
        
        logger.info("Inventory check completed for order: {}. Approved: {}, Missing items: {}", 
                   orderId, allItemsAvailable, missingItems.size());
        
        return result;
    }
    
    private MissingItem checkItemAvailability(OrderItem item) {
        String productId = item.getProductId();
        int requestedQuantity = item.getQuantity();
        String category = item.getCategory();
        
        ProductInfo productInfo = productCatalog.get(productId);
        
        if (productInfo == null) {
            logger.warn("Product not found in catalog: {}", productId);
            return new MissingItem(
                productId, requestedQuantity, 0, "Product not found in catalog"
            );
        }
        
        // Check if category matches
        if (!category.equals(productInfo.getCategory())) {
            logger.warn("Category mismatch for product: {}. Expected: {}, Actual: {}", 
                       productId, productInfo.getCategory(), category);
            return new MissingItem(
                productId, requestedQuantity, productInfo.getAvailableQuantity(), 
                "Category mismatch. Expected: " + productInfo.getCategory() + ", Actual: " + category
            );
        }
        
        // Apply category-specific rules
        switch (category.toLowerCase()) {
            case "standard":
                return checkStandardProduct(productId, requestedQuantity, productInfo);
                
            case "perishable":
                return checkPerishableProduct(productId, requestedQuantity, productInfo);
                
            case "digital":
                return checkDigitalProduct(productId, requestedQuantity, productInfo);
                
            default:
                logger.warn("Unknown category for product: {}", productId);
                return new MissingItem(
                    productId, requestedQuantity, productInfo.getAvailableQuantity(), 
                    "Unknown category: " + category
                );
        }
    }
    
    private MissingItem checkStandardProduct(String productId, 
                                           int requestedQuantity, 
                                           ProductInfo productInfo) {
        if (productInfo.getAvailableQuantity() >= requestedQuantity) {
            logger.debug("Standard product available: {} (requested: {}, available: {})", 
                        productId, requestedQuantity, productInfo.getAvailableQuantity());
            return null; // Item is available
        } else {
            logger.warn("Insufficient quantity for standard product: {} (requested: {}, available: {})", 
                       productId, requestedQuantity, productInfo.getAvailableQuantity());
            return new MissingItem(
                productId, requestedQuantity, productInfo.getAvailableQuantity(), 
                "Insufficient quantity"
            );
        }
    }
    
    private MissingItem checkPerishableProduct(String productId, 
                                             int requestedQuantity, 
                                             ProductInfo productInfo) {
        LocalDate currentDate = LocalDate.now();
        
        // Check if product is expired
        if (productInfo.getExpirationDate() != null && 
            productInfo.getExpirationDate().isBefore(currentDate)) {
            logger.warn("Perishable product expired: {} (expiration: {}, current: {})", 
                       productId, productInfo.getExpirationDate(), currentDate);
            return new MissingItem(
                productId, requestedQuantity, productInfo.getAvailableQuantity(), 
                "Product expired on " + productInfo.getExpirationDate()
            );
        }
        
        // Check quantity
        if (productInfo.getAvailableQuantity() >= requestedQuantity) {
            logger.debug("Perishable product available: {} (requested: {}, available: {}, expiration: {})", 
                        productId, requestedQuantity, productInfo.getAvailableQuantity(), 
                        productInfo.getExpirationDate());
            return null; // Item is available
        } else {
            logger.warn("Insufficient quantity for perishable product: {} (requested: {}, available: {})", 
                       productId, requestedQuantity, productInfo.getAvailableQuantity());
            return new MissingItem(
                productId, requestedQuantity, productInfo.getAvailableQuantity(), 
                "Insufficient quantity"
            );
        }
    }
    
    private MissingItem checkDigitalProduct(String productId, 
                                           int requestedQuantity, 
                                           ProductInfo productInfo) {
        // Digital products are always considered available
        logger.debug("Digital product available: {} (requested: {})", productId, requestedQuantity);
        return null; // Item is available
    }
    
    public Map<String, ProductInfo> getProductCatalog() {
        return new HashMap<>(productCatalog);
    }
    
    private void storeMissingItemsInRedis(String orderId, List<MissingItem> missingItems) {
        try {
            String missingItemsJson = objectMapper.writeValueAsString(missingItems);
            String redisKey = "missingItems:" + orderId;
            
            redisTemplate.opsForValue().set(redisKey, missingItemsJson, REDIS_TTL);
            logger.debug("Missing items stored in Redis with key: {}", redisKey);
            
        } catch (JsonProcessingException e) {
            logger.error("Error serializing missing items for Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store missing items in Redis", e);
        }
    }
    
    private void updateOrderStatusInRedis(String orderId, OrderStatus status) {
        try {
            String orderJson = redisTemplate.opsForValue().get("order:" + orderId);
            if (orderJson == null) {
                logger.warn("Order not found in Redis for status update: {}", orderId);
                return;
            }
            
            Order order = objectMapper.readValue(orderJson, Order.class);
            order.setStatus(status);
            
            // Update order in Redis
            String updatedOrderJson = objectMapper.writeValueAsString(order);
            redisTemplate.opsForValue().set("order:" + orderId, updatedOrderJson, REDIS_TTL);
            
            logger.info("Order status updated in Redis. ID: {}, New Status: {}", orderId, status);
            
        } catch (JsonProcessingException e) {
            logger.error("Error updating order status in Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Error updating order status in Redis", e);
        }
    }
}

