package com.example.order.service;

import com.example.order.client.InventoryClient;
import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.exception.InsufficientStockException;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private InventoryClient inventoryClient;

	@InjectMocks
	private OrderService orderService;

	@Test
	void confirmsOrderWhenReservationSucceeds() {
		CreateOrderRequest request = new CreateOrderRequest(1L, 2);
		when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Order order = orderService.create(request);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
		verify(inventoryClient).reserveStock(1L, 2);
	}

	@Test
	void rejectsAndPersistsOrderWhenStockIsInsufficient() {
		CreateOrderRequest request = new CreateOrderRequest(1L, 100);
		doThrow(new InsufficientStockException("no stock")).when(inventoryClient).reserveStock(1L, 100);
		when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		assertThatThrownBy(() -> orderService.create(request))
				.isInstanceOf(InsufficientStockException.class);

		ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
		verify(orderRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.REJECTED);
	}
}
