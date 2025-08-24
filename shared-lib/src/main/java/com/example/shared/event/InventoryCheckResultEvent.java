package com.example.shared.event;

import com.example.shared.dto.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class InventoryCheckResultEvent {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("status")
    private OrderStatus status;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    // Default constructor
    public InventoryCheckResultEvent() {}
    
    // Constructor with all fields
    public InventoryCheckResultEvent(String orderId, OrderStatus status, Instant timestamp) {
        this.orderId = orderId;
        this.status = status;
        this.timestamp = timestamp;
    }
    
    // Constructor with current timestamp
    public InventoryCheckResultEvent(String orderId, OrderStatus status) {
        this(orderId, status, Instant.now());
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
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "InventoryCheckResultEvent{" +
                "orderId='" + orderId + '\'' +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }
}

