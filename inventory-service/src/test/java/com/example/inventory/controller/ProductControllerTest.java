package com.example.inventory.controller;

import com.example.inventory.domain.Product;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	@Test
	void reserveBeyondStockReturns409() throws Exception {
		when(productService.reserve(eq(1L), anyInt()))
				.thenThrow(new InsufficientStockException("Not enough stock for product SKU-1: requested 5, available 2"));

		mockMvc.perform(post("/api/products/1/reserve")
						.contentType("application/json")
						.content("{\"quantity\": 5}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void reserveWithinStockReturns200() throws Exception {
		Product reserved = Product.builder().id(1L).sku("SKU-1").name("Widget").stock(6).build();
		when(productService.reserve(eq(1L), anyInt())).thenReturn(reserved);

		mockMvc.perform(post("/api/products/1/reserve")
						.contentType("application/json")
						.content("{\"quantity\": 4}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stock").value(6));
	}

	@Test
	void reserveWithZeroQuantityReturns400() throws Exception {
		mockMvc.perform(post("/api/products/1/reserve")
						.contentType("application/json")
						.content("{\"quantity\": 0}"))
				.andExpect(status().isBadRequest());
	}
}
