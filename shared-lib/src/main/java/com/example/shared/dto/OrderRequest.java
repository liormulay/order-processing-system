package com.example.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class OrderRequest {
    
    @NotBlank(message = "Customer name is required")
    @JsonProperty("customerName")
    private String customerName;
    
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    @JsonProperty("items")
    private List<OrderItem> items;
    
    @NotNull(message = "Request timestamp is required")
    @JsonProperty("requestedAt")
    private Instant requestedAt;
    
    // Default constructor
    public OrderRequest() {}
    
    // Constructor with all fields
    public OrderRequest(String customerName, List<OrderItem> items, Instant requestedAt) {
        this.customerName = customerName;
        this.items = items;
        this.requestedAt = requestedAt;
    }
    
    // Getters and Setters
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public List<OrderItem> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
    
    public Instant getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    @Override
    public String toString() {
        return "OrderRequest{" +
                "customerName='" + customerName + '\'' +
                ", items=" + items +
                ", requestedAt=" + requestedAt +
                '}';
    }
}

