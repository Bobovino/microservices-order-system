package com.example.inventory.controller;

import com.example.inventory.dto.CreateProductRequest;
import com.example.inventory.dto.ProductResponse;
import com.example.inventory.dto.ReserveStockRequest;
import com.example.inventory.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
		return ProductResponse.from(productService.create(request));
	}

	@GetMapping
	public List<ProductResponse> list() {
		return productService.list().stream().map(ProductResponse::from).toList();
	}

	@GetMapping("/{id}")
	public ProductResponse getById(@PathVariable Long id) {
		return ProductResponse.from(productService.getById(id));
	}

	@PostMapping("/{id}/reserve")
	public ProductResponse reserve(@PathVariable Long id, @Valid @RequestBody ReserveStockRequest request) {
		return ProductResponse.from(productService.reserve(id, request.quantity()));
	}
}
