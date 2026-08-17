package com.example.order.client;

import com.example.order.exception.InsufficientStockException;
import com.example.order.exception.InventoryServiceUnavailableException;
import com.example.order.exception.ProductNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Component
public class WebClientInventoryClient implements InventoryClient {

	private final WebClient webClient;

	public WebClientInventoryClient(WebClient.Builder webClientBuilder, InventoryClientProperties properties) {
		this.webClient = webClientBuilder.baseUrl(properties.baseUrl()).build();
	}

	@Override
	public void reserveStock(Long productId, int quantity) {
		try {
			webClient.post()
					.uri("/api/products/{id}/reserve", productId)
					.bodyValue(Map.of("quantity", quantity))
					.retrieve()
					.toBodilessEntity()
					.block();
		} catch (WebClientResponseException.NotFound ex) {
			throw new ProductNotFoundException("Product not found: " + productId);
		} catch (WebClientResponseException.Conflict ex) {
			throw new InsufficientStockException("Insufficient stock for product " + productId);
		} catch (WebClientRequestException ex) {
			throw new InventoryServiceUnavailableException("Inventory service is unreachable", ex);
		}
	}
}
