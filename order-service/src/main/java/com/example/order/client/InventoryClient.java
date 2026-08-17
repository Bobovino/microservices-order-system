package com.example.order.client;

public interface InventoryClient {

	void reserveStock(Long productId, int quantity);
}
