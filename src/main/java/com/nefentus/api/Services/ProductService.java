package com.nefentus.api.Services;

import com.nefentus.api.Errors.*;
import com.nefentus.api.entities.*;
import com.nefentus.api.payload.request.AddProductRequest;
import com.nefentus.api.payload.response.*;
import com.nefentus.api.repositories.ProductRepository;
import com.nefentus.api.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	public List<Product> getProducts(String username) throws UserNotFoundException {
		User user = userRepository.findUserByEmail(username)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));
		List<Product> products = productRepository.findByUser(user);
		return products;
	}

	public boolean addProduct(AddProductRequest request, String username) throws UserNotFoundException {
		User user = userRepository.findUserByEmail(username)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));

		Product product = new Product();
		product.setName(request.getName());
		product.setDescription(request.getDescription());
		product.setPrice(request.getPrice());
		product.setImagePath(request.getImagePath() != null ? request.getImagePath() : "");
		product.setStock(request.getStock());
		product.setUser(user);
		product.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
		product.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

		productRepository.save(product);

		return true;
	}
}