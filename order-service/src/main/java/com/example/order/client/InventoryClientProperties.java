package com.example.order.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory-service")
public record InventoryClientProperties(String baseUrl) {
}
