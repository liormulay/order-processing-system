package com.example.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MissingItem {
    
    @JsonProperty("productId")
    private String productId;
    
    @JsonProperty("requestedQuantity")
    private Integer requestedQuantity;
    
    @JsonProperty("availableQuantity")
    private Integer availableQuantity;
    
    @JsonProperty("reason")
    private String reason;
    
    // Default constructor
    public MissingItem() {}
    
    // Constructor with all fields
    public MissingItem(String productId, Integer requestedQuantity, 
                      Integer availableQuantity, String reason) {
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
        this.reason = reason;
    }
    
    // Getters and Setters
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }
    
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
    
    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    @Override
    public String toString() {
        return "MissingItem{" +
                "productId='" + productId + '\'' +
                ", requestedQuantity=" + requestedQuantity +
                ", availableQuantity=" + availableQuantity +
                ", reason='" + reason + '\'' +
                '}';
    }
}
