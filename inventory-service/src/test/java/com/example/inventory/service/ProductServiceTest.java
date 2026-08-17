package com.example.inventory.service;

import com.example.inventory.domain.Product;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ProductService productService;

	@Test
	void reservingWithinStockDecrementsIt() {
		Product product = Product.builder().id(1L).sku("SKU-1").name("Widget").stock(10).build();
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		Product result = productService.reserve(1L, 4);

		assertThat(result.getStock()).isEqualTo(6);
	}

	@Test
	void reservingMoreThanAvailableThrows() {
		Product product = Product.builder().id(1L).sku("SKU-1").name("Widget").stock(2).build();
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.reserve(1L, 5))
				.isInstanceOf(InsufficientStockException.class);
	}
}
