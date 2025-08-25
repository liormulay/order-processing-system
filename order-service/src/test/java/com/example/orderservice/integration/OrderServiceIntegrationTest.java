package com.example.orderservice.integration;

import com.example.shared.dto.OrderRequest;
import com.example.shared.dto.OrderItem;
import com.example.shared.dto.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Test
    void testCreateOrder_AllProductsAvailable_OrderApproved() throws Exception {
        // Arrange
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        OrderRequest orderRequest = new OrderRequest(
            "Test Customer",
            Arrays.asList(
                new OrderItem("P1001", 2, "standard"),  // Available in catalog
                new OrderItem("P1003", 1, "digital")    // Digital product, always available
            ),
            Instant.now()
        );

        // Act & Assert
        String response = mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Order received and being processed"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract order ID from response
        String orderId = objectMapper.readTree(response).get("orderId").asText();
        
        // Wait a bit for async processing
        Thread.sleep(2000);
        
        // Check order status - should be APPROVED
        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void testCreateOrder_InvalidRequest_BadRequest() throws Exception {
        // Arrange
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Invalid request - missing required fields
        String invalidJson = "{\"customerName\":\"Test\"}";

        // Act & Assert
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetOrderStatus_OrderNotFound_NotFound() throws Exception {
        // Arrange
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        String nonExistentOrderId = "ORD-NOTFOUND";

        // Act & Assert
        mockMvc.perform(get("/orders/" + nonExistentOrderId))
                .andExpect(status().isNotFound());
    }
}
