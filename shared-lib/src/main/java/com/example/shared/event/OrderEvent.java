package com.example.shared.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class OrderEvent {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    // Default constructor
    public OrderEvent() {}
    
    // Constructor with all fields
    public OrderEvent(String orderId, Instant timestamp) {
        this.orderId = orderId;
        this.timestamp = timestamp;
    }
    
    // Constructor with current timestamp
    public OrderEvent(String orderId) {
        this(orderId, Instant.now());
    }
    
    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "OrderEvent{" +
                "orderId='" + orderId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

