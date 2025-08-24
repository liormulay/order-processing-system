package com.example.inventoryservice.model;

import java.time.LocalDate;

public class ProductInfo {
    private String category;
    private int availableQuantity;
    private LocalDate expirationDate;
    
    // Default constructor
    public ProductInfo() {}
    
    // Constructor with all fields
    public ProductInfo(String category, int availableQuantity, LocalDate expirationDate) {
        this.category = category;
        this.availableQuantity = availableQuantity;
        this.expirationDate = expirationDate;
    }
    
    // Getters and Setters
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getAvailableQuantity() {
        return availableQuantity;
    }
    
    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
    
    public LocalDate getExpirationDate() {
        return expirationDate;
    }
    
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    @Override
    public String toString() {
        return "ProductInfo{" +
                "category='" + category + '\'' +
                ", availableQuantity=" + availableQuantity +
                ", expirationDate=" + expirationDate +
                '}';
    }
}

