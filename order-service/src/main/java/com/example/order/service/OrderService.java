package com.example.order.service;

import com.example.order.client.InventoryClient;
import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.exception.InsufficientStockException;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.exception.ProductNotFoundException;
import com.example.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderService {

	private final OrderRepository orderRepository;
	private final InventoryClient inventoryClient;

	public OrderService(OrderRepository orderRepository, InventoryClient inventoryClient) {
		this.orderRepository = orderRepository;
		this.inventoryClient = inventoryClient;
	}

	@Transactional(noRollbackFor = {InsufficientStockException.class, ProductNotFoundException.class})
	public Order create(CreateOrderRequest request) {
		try {
			inventoryClient.reserveStock(request.productId(), request.quantity());
		} catch (InsufficientStockException | ProductNotFoundException ex) {
			orderRepository.save(toOrder(request, OrderStatus.REJECTED));
			throw ex;
		}
		return orderRepository.save(toOrder(request, OrderStatus.CONFIRMED));
	}

	public List<Order> list() {
		return orderRepository.findAll();
	}

	public Order getById(Long id) {
		return orderRepository.findById(id)
				.orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
	}

	private Order toOrder(CreateOrderRequest request, OrderStatus status) {
		return Order.builder()
				.productId(request.productId())
				.quantity(request.quantity())
				.status(status)
				.createdAt(Instant.now())
				.build();
	}
}
