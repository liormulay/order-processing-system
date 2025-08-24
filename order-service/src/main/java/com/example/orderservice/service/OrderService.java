package com.example.orderservice.service;

import com.example.shared.dto.Order;
import com.example.shared.dto.OrderRequest;
import com.example.shared.dto.OrderStatus;
import com.example.shared.event.OrderEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static final Duration REDIS_TTL = Duration.ofHours(24); // 24 hours TTL
    
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;
    

    
    @Autowired
    public OrderService(KafkaTemplate<String, OrderEvent> kafkaTemplate,
                       RedisTemplate<String, String> redisTemplate,
                       ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    public String processOrder(OrderRequest orderRequest) {
        String orderId = generateOrderId();
        logger.info("Processing order with ID: {}", orderId);
        
        try {
            // Create order with PENDING status
            Order order = new Order(orderId, orderRequest, OrderStatus.PENDING);
            
            // Store order in Redis
            storeOrderInRedis(order);
            
            // Create and publish order event to Kafka (only orderId)
            OrderEvent orderEvent = new OrderEvent(orderId);
            publishOrderEvent(orderEvent);
            
            logger.info("Order processed successfully. ID: {}, Status: {}", orderId, OrderStatus.PENDING);
            return orderId;
            
        } catch (Exception e) {
            logger.error("Failed to process order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process order: " + e.getMessage(), e);
        }
    }
    
    public OrderStatus getOrderStatus(String orderId) {
        try {
            String orderJson = redisTemplate.opsForValue().get("order:" + orderId);
            if (orderJson == null) {
                throw new RuntimeException("Order not found: " + orderId);
            }
            
            Order order = objectMapper.readValue(orderJson, Order.class);
            return order.getStatus();
            
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing order from Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving order status", e);
        }
    }
    

    
    private void storeOrderInRedis(Order order) {
        try {
            String orderJson = objectMapper.writeValueAsString(order);
            String redisKey = "order:" + order.getOrderId();
            
            redisTemplate.opsForValue().set(redisKey, orderJson, REDIS_TTL);
            logger.debug("Order stored in Redis with key: {}", redisKey);
            
        } catch (JsonProcessingException e) {
            logger.error("Error serializing order for Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store order in Redis", e);
        }
    }
    
    private void publishOrderEvent(OrderEvent orderEvent) {
        try {
            CompletableFuture<SendResult<String, OrderEvent>> future = 
                kafkaTemplate.send(orderEventsTopic, orderEvent.getOrderId(), orderEvent);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Order event published successfully. Order ID: {}, Topic: {}, Partition: {}, Offset: {}", 
                              orderEvent.getOrderId(), result.getRecordMetadata().topic(), 
                              result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish order event. Order ID: {}", orderEvent.getOrderId(), ex);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error publishing order event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish order event", e);
        }
    }
    
    private String generateOrderId() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

