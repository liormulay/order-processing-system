package com.example.orderservice.service;

import com.example.shared.dto.OrderRequest;
import com.example.shared.dto.OrderItem;
import com.example.shared.dto.OrderStatus;
import com.example.shared.event.OrderEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SendResult<String, OrderEvent> sendResult;

    @Captor
    private ArgumentCaptor<String> redisKeyCaptor;

    @Captor
    private ArgumentCaptor<String> redisValueCaptor;

    @Captor
    private ArgumentCaptor<OrderEvent> orderEventCaptor;

    private OrderService orderService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orderService = new OrderService(kafkaTemplate, redisTemplate, objectMapper);
        ReflectionTestUtils.setField(orderService, "orderEventsTopic", "order-events");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testProcessOrder_AllProductsAvailable_OrderApproved() throws Exception {
        // Arrange
        OrderRequest orderRequest = createValidOrderRequest();
        CompletableFuture<SendResult<String, OrderEvent>> future = 
            CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(anyString(), anyString(), any(OrderEvent.class)))
            .thenReturn(future);

        // Act
        String orderId = orderService.processOrder(orderRequest);

        // Assert
        assertNotNull(orderId);
        assertTrue(orderId.startsWith("ORD-"));
        assertEquals(12, orderId.length()); // "ORD-" + 8 characters

        // Verify Redis storage
        verify(valueOperations).set(redisKeyCaptor.capture(), redisValueCaptor.capture(), any());
        
        String capturedKey = redisKeyCaptor.getValue();
        String capturedValue = redisValueCaptor.getValue();
        
        assertTrue(capturedKey.startsWith("order:"));
        assertTrue(capturedKey.endsWith(orderId));
        
        // Verify the stored order has PENDING status
        assertTrue(capturedValue.contains("\"status\":\"PENDING\""));

        // Verify Kafka event publication
        verify(kafkaTemplate).send(eq("order-events"), eq(orderId), orderEventCaptor.capture());
        
        OrderEvent capturedEvent = orderEventCaptor.getValue();
        assertEquals(orderId, capturedEvent.getOrderId());
        assertNotNull(capturedEvent.getTimestamp());
    }

    @Test
    void testGetOrderStatus_OrderExists_ReturnsStatus() throws Exception {
        // Arrange
        String orderId = "ORD-TEST123";
        String orderJson = "{\"orderId\":\"" + orderId + "\",\"status\":\"APPROVED\"}";
        
        when(valueOperations.get("order:" + orderId)).thenReturn(orderJson);

        // Act
        OrderStatus status = orderService.getOrderStatus(orderId);

        // Assert
        assertEquals(OrderStatus.APPROVED, status);
        verify(valueOperations).get("order:" + orderId);
    }

    @Test
    void testGetOrderStatus_OrderNotFound_ThrowsException() {
        // Arrange
        String orderId = "ORD-NOTFOUND";
        when(valueOperations.get("order:" + orderId)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.getOrderStatus(orderId));
        
        assertTrue(exception.getMessage().contains("Order not found"));
        verify(valueOperations).get("order:" + orderId);
    }

    @Test
    void testProcessOrder_RedisFailure_ThrowsException() {
        // Arrange
        OrderRequest orderRequest = createValidOrderRequest();
        doThrow(new RuntimeException("Redis connection failed"))
            .when(valueOperations).set(anyString(), anyString(), any());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.processOrder(orderRequest));
        
        assertTrue(exception.getMessage().contains("Failed to process order"));
        verify(valueOperations).set(anyString(), anyString(), any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void testProcessOrder_KafkaFailure_ThrowsException() {
        // Arrange
        OrderRequest orderRequest = createValidOrderRequest();
        doThrow(new RuntimeException("Kafka connection failed"))
            .when(kafkaTemplate).send(anyString(), anyString(), any(OrderEvent.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.processOrder(orderRequest));
        
        assertTrue(exception.getMessage().contains("Failed to process order"));
        verify(valueOperations).set(anyString(), anyString(), any());
        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }

    private OrderRequest createValidOrderRequest() {
        OrderItem item1 = new OrderItem("P1001", 2, "standard");
        OrderItem item2 = new OrderItem("P1003", 1, "digital");
        
        return new OrderRequest(
            "Test Customer",
            Arrays.asList(item1, item2),
            Instant.now()
        );
    }
}
