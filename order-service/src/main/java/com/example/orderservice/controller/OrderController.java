package com.example.orderservice.controller;

import com.example.shared.dto.OrderRequest;
import com.example.shared.dto.OrderStatus;
import com.example.shared.event.OrderEvent;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        logger.info("Received order request: {}", orderRequest);
        
        try {
            String orderId = orderService.processOrder(orderRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("status", OrderStatus.PENDING);
            response.put("message", "Order received and being processed");
            
            logger.info("Order created successfully with ID: {}", orderId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Error processing order: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to process order");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable String orderId) {
        logger.info("Checking status for order: {}", orderId);
        
        try {
            OrderStatus status = orderService.getOrderStatus(orderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("status", status);
            
            logger.info("Order status retrieved successfully. Order ID: {}, Status: {}", orderId, status);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving order status: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve order status");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

}

