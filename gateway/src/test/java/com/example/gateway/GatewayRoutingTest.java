package com.example.gateway;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayRoutingTest {

	static final MockWebServer inventoryStub = new MockWebServer();
	static final MockWebServer orderStub = new MockWebServer();

	@DynamicPropertySource
	static void backendUrls(DynamicPropertyRegistry registry) throws IOException {
		inventoryStub.start();
		orderStub.start();
		registry.add("INVENTORY_SERVICE_URL", () -> "http://localhost:" + inventoryStub.getPort());
		registry.add("ORDER_SERVICE_URL", () -> "http://localhost:" + orderStub.getPort());
	}

	@AfterAll
	static void shutdown() throws IOException {
		inventoryStub.shutdown();
		orderStub.shutdown();
	}

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void routesProductRequestsToInventoryService() {
		inventoryStub.enqueue(new MockResponse().setBody("[]").addHeader("Content-Type", "application/json"));

		webTestClient.get().uri("/api/products")
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	void routesOrderRequestsToOrderService() {
		orderStub.enqueue(new MockResponse().setBody("[]").addHeader("Content-Type", "application/json"));

		webTestClient.get().uri("/api/orders")
				.exchange()
				.expectStatus().isOk();
	}
}
