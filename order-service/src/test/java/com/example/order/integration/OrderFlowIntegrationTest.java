package com.example.order.integration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	static final MockWebServer inventoryStub = new MockWebServer();

	@DynamicPropertySource
	static void inventoryServiceUrl(DynamicPropertyRegistry registry) throws IOException {
		inventoryStub.start();
		registry.add("inventory-service.base-url", () -> "http://localhost:" + inventoryStub.getPort());
	}

	@AfterAll
	static void shutdownStub() throws IOException {
		inventoryStub.shutdown();
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void confirmsOrderWhenInventoryAcceptsReservation() throws Exception {
		inventoryStub.enqueue(new MockResponse().setResponseCode(200));

		mockMvc.perform(post("/api/orders")
						.contentType("application/json")
						.content("{\"productId\": 1, \"quantity\": 2}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("CONFIRMED"));
	}

	@Test
	void rejectsOrderWhenInventoryReturnsConflict() throws Exception {
		inventoryStub.enqueue(new MockResponse().setResponseCode(409));

		mockMvc.perform(post("/api/orders")
						.contentType("application/json")
						.content("{\"productId\": 1, \"quantity\": 999}"))
				.andExpect(status().isConflict());
	}

	@Test
	void persistsRejectedOrderForAudit() throws Exception {
		inventoryStub.enqueue(new MockResponse().setResponseCode(409));

		mockMvc.perform(post("/api/orders")
						.contentType("application/json")
						.content("{\"productId\": 1, \"quantity\": 999}"))
				.andExpect(status().isConflict());

		mockMvc.perform(get("/api/orders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.status == 'REJECTED')]").isNotEmpty());
	}
}
