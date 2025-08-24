package com.example.inventoryservice.listener;

import com.example.shared.dto.Order;
import com.example.shared.dto.OrderItem;
import com.example.shared.event.OrderEvent;
import com.example.shared.event.InventoryCheckResultEvent;
import com.example.inventoryservice.service.InventoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class OrderEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);
    
    private final InventoryService inventoryService;
    private final KafkaTemplate<String, InventoryCheckResultEvent> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${kafka.topics.inventory-check-results}")
    private String inventoryCheckResultsTopic;
    
    @Autowired
    public OrderEventListener(InventoryService inventoryService,
                            KafkaTemplate<String, InventoryCheckResultEvent> kafkaTemplate,
                            RedisTemplate<String, String> redisTemplate,
                            ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "${kafka.topics.order-events}", 
                  groupId = "inventory-service-group",
                  containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderEvent(OrderEvent orderEvent) {
        logger.info("Received order event for order: {}", orderEvent.getOrderId());
        
        try {
            // Fetch order data from Redis using orderId
            Order order = fetchOrderFromRedis(orderEvent.getOrderId());
            
            // Extract order items from the order
            List<OrderItem> items = order.getItems();
            
            // Perform inventory check
            InventoryCheckResultEvent result = inventoryService.checkInventory(
                orderEvent.getOrderId(), items
            );
            
            // Publish inventory check result
            publishInventoryCheckResult(result);
            
        } catch (Exception e) {
            logger.error("Error processing order event for order {}: {}", 
                        orderEvent.getOrderId(), e.getMessage(), e);
        }
    }
    
    private Order fetchOrderFromRedis(String orderId) {
        try {
            String orderJson = redisTemplate.opsForValue().get("order:" + orderId);
            if (orderJson == null) {
                throw new RuntimeException("Order not found in Redis: " + orderId);
            }
            
            Order order = objectMapper.readValue(orderJson, Order.class);
            logger.debug("Retrieved order from Redis: {}", orderId);
            return order;
            
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing order from Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving order from Redis", e);
        }
    }
    
    private void publishInventoryCheckResult(InventoryCheckResultEvent result) {
        try {
            CompletableFuture<SendResult<String, InventoryCheckResultEvent>> future = 
                kafkaTemplate.send(inventoryCheckResultsTopic, result.getOrderId(), result);
            
            future.whenComplete((sendResult, ex) -> {
                if (ex == null) {
                    logger.info("Inventory check result published successfully. Order ID: {}, Topic: {}, Partition: {}, Offset: {}", 
                              result.getOrderId(), sendResult.getRecordMetadata().topic(), 
                              sendResult.getRecordMetadata().partition(), sendResult.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish inventory check result. Order ID: {}", result.getOrderId(), ex);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error publishing inventory check result: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish inventory check result", e);
        }
    }
}

