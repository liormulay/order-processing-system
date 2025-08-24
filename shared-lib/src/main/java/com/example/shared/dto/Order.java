package com.example.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public class Order extends OrderRequest {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("status")
    private OrderStatus status;
    
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    // Default constructor
    public Order() {
        super();
    }
    
    // Constructor with all fields
    public Order(String orderId, String customerName, List<OrderItem> items, 
                Instant requestedAt, OrderStatus status, Instant createdAt) {
        super(customerName, items, requestedAt);
        this.orderId = orderId;
        this.status = status;
        this.createdAt = createdAt;
    }
    
    // Constructor from OrderRequest
    public Order(String orderId, OrderRequest request, OrderStatus status) {
        super(request.getCustomerName(), request.getItems(), request.getRequestedAt());
        this.orderId = orderId;
        this.status = status;
        this.createdAt = Instant.now();
    }
    
    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", customerName='" + getCustomerName() + '\'' +
                ", items=" + getItems() +
                ", requestedAt=" + getRequestedAt() +
                '}';
    }
}

