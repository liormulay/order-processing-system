package com.example.notificationservice.service;

import com.example.shared.dto.Order;
import com.example.shared.dto.MissingItem;
import com.example.shared.event.InventoryCheckResultEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public NotificationService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    public void processInventoryCheckResult(InventoryCheckResultEvent event) {
        logger.info("Processing inventory check result for order: {}", event.getOrderId());
        
        try {
            // Retrieve the original order from Redis
            Order order = retrieveOrderFromRedis(event.getOrderId());
            
            if (order == null) {
                logger.error("Order not found in Redis for notification: {}", event.getOrderId());
                return;
            }
            
            // Log appropriate notification based on result
            if (event.getStatus() == com.example.shared.dto.OrderStatus.APPROVED) {
                logOrderConfirmation(order, event);
            } else {
                logOrderRejection(order, event);
            }
            
        } catch (Exception e) {
            logger.error("Error processing inventory check result for order {}: {}", 
                        event.getOrderId(), e.getMessage(), e);
        }
    }
    
    private Order retrieveOrderFromRedis(String orderId) {
        try {
            String orderJson = redisTemplate.opsForValue().get("order:" + orderId);
            if (orderJson == null) {
                logger.warn("Order not found in Redis: {}", orderId);
                return null;
            }
            
            return objectMapper.readValue(orderJson, Order.class);
            
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing order from Redis: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private List<MissingItem> retrieveMissingItemsFromRedis(String orderId) {
        try {
            String missingItemsJson = redisTemplate.opsForValue().get("missingItems:" + orderId);
            if (missingItemsJson == null) {
                logger.debug("No missing items found in Redis for order: {}", orderId);
                return null;
            }
            
            return objectMapper.readValue(missingItemsJson, new TypeReference<List<MissingItem>>() {});
            
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing missing items from Redis: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private void logOrderConfirmation(Order order, InventoryCheckResultEvent event) {
        logger.info("=== ORDER CONFIRMATION ===");
        logger.info("Order ID: {}", order.getOrderId());
        logger.info("Customer: {}", order.getCustomerName());
        logger.info("Status: APPROVED");
        logger.info("Items:");
        
        order.getItems().forEach(item -> 
            logger.info("  - Product: {}, Quantity: {}, Category: {}", 
                      item.getProductId(), item.getQuantity(), item.getCategory())
        );
        
        logger.info("Requested at: {}", order.getRequestedAt());
        logger.info("Processed at: {}", event.getTimestamp());
        logger.info("========================");
    }
    
    private void logOrderRejection(Order order, InventoryCheckResultEvent event) {
        logger.warn("=== ORDER REJECTION ===");
        logger.warn("Order ID: {}", order.getOrderId());
        logger.warn("Customer: {}", order.getCustomerName());
        logger.warn("Status: REJECTED");
        logger.warn("Missing/Unavailable Items:");
        
        List<MissingItem> missingItems = retrieveMissingItemsFromRedis(event.getOrderId());
        if (missingItems != null && !missingItems.isEmpty()) {
            missingItems.forEach(missingItem -> 
                logger.warn("  - Product: {}, Requested: {}, Available: {}, Reason: {}", 
                          missingItem.getProductId(), missingItem.getRequestedQuantity(), 
                          missingItem.getAvailableQuantity(), missingItem.getReason())
            );
        } else {
            logger.warn("  - No specific missing items information available");
        }
        
        logger.warn("Requested at: {}", order.getRequestedAt());
        logger.warn("Processed at: {}", event.getTimestamp());
        logger.warn("=======================");
    }
}

