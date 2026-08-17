package com.example.order.controller;

import com.example.order.exception.InsufficientStockException;
import com.example.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderService orderService;

	@Test
	void createOrderWithZeroQuantityReturns400() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType("application/json")
						.content("{\"productId\": 1, \"quantity\": 0}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createOrderPropagatesInsufficientStockAs409() throws Exception {
		when(orderService.create(any())).thenThrow(new InsufficientStockException("no stock"));

		mockMvc.perform(post("/api/orders")
						.contentType("application/json")
						.content("{\"productId\": 1, \"quantity\": 5}"))
				.andExpect(status().isConflict());
	}
}
