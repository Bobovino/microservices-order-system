package com.example.inventory.dto;

import com.example.inventory.domain.Product;

public record ProductResponse(
		Long id,
		String sku,
		String name,
		int stock
) {
	public static ProductResponse from(Product product) {
		return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getStock());
	}
}
