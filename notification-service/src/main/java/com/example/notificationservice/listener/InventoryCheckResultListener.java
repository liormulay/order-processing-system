package com.example.notificationservice.listener;

import com.example.shared.event.InventoryCheckResultEvent;
import com.example.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryCheckResultListener {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryCheckResultListener.class);
    
    private final NotificationService notificationService;
    
    @Autowired
    public InventoryCheckResultListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @KafkaListener(topics = "${kafka.topics.inventory-check-results}", 
                  groupId = "notification-service-group",
                  containerFactory = "kafkaListenerContainerFactory")
    public void handleInventoryCheckResult(InventoryCheckResultEvent event) {
        logger.info("Received inventory check result for notification processing. Order ID: {}", event.getOrderId());
        
        try {
            notificationService.processInventoryCheckResult(event);
        } catch (Exception e) {
            logger.error("Error processing inventory check result for notification. Order ID: {}", 
                        event.getOrderId(), e);
        }
    }
}

