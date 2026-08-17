package com.example.inventory.service;

import com.example.inventory.domain.Product;
import com.example.inventory.dto.CreateProductRequest;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.exception.ProductNotFoundException;
import com.example.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Transactional
	public Product create(CreateProductRequest request) {
		Product product = Product.builder()
				.sku(request.sku())
				.name(request.name())
				.stock(request.stock())
				.build();
		return productRepository.save(product);
	}

	public List<Product> list() {
		return productRepository.findAll();
	}

	public Product getById(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
	}

	@Transactional
	public Product reserve(Long id, int quantity) {
		Product product = getById(id);
		if (product.getStock() < quantity) {
			throw new InsufficientStockException(
					"Not enough stock for product " + product.getSku() + ": requested " + quantity + ", available " + product.getStock());
		}
		product.setStock(product.getStock() - quantity);
		return product;
	}
}
