package com.example.inventory.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ProductIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createAndReserveProductEndToEnd() throws Exception {
		Map<String, Object> createRequest = Map.of("sku", "SKU-100", "name", "Mechanical Keyboard", "stock", 5);

		String createResponse = mockMvc.perform(post("/api/products")
						.contentType("application/json")
						.content(objectMapper.writeValueAsString(createRequest)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		Long productId = objectMapper.readTree(createResponse).get("id").asLong();

		mockMvc.perform(post("/api/products/" + productId + "/reserve")
						.contentType("application/json")
						.content("{\"quantity\": 3}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stock").value(2));

		mockMvc.perform(post("/api/products/" + productId + "/reserve")
						.contentType("application/json")
						.content("{\"quantity\": 3}"))
				.andExpect(status().isConflict());
	}
}
