package com.nefentus.api.Services;

import com.nefentus.api.Errors.*;
import com.nefentus.api.entities.*;
import com.nefentus.api.payload.request.DeleteProductRequest;
import com.nefentus.api.repositories.ProductRepository;
import com.nefentus.api.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	@Autowired
	private S3Service s3Service;

	public List<Product> getProducts(String username) throws UserNotFoundException {
		User user = userRepository.findUserByEmail(username)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));
		List<Product> products = productRepository.findByUser(user);
		return products;
	}

	public Optional<Product> getProductByLink(String link) {
		return productRepository.findByLink(link);
	}

	public Optional<String> getProductImageByLink(String link) {
		Optional<Product> optProduct = productRepository.findByLink(link);
		if (optProduct.isPresent()) {
			Product product = optProduct.get();
			return this.getProductImageUrl(product.getId());
		}
		return Optional.empty();
	}

	public Product upsertProduct(long productId, String name, String description, BigDecimal price, int stock,
			String username) throws ApiRequestException, IOException, UserNotFoundException {
		Optional<Product> optProduct = productRepository.findById(productId);

		Product product;
		if (optProduct.isPresent()) {
			product = optProduct.get();
			// Check if user is owner of product
			if (!product.getUser().getEmail().equals(username)) {
				log.error("User not owner of product: " + username);
				throw new ApiRequestException("User not owner of product: " + username, HttpStatus.FORBIDDEN,
						ZonedDateTime.now());
			}
		} else {
			product = new Product();

			User owner = userRepository.findUserByEmail(username)
					.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));
			product.setUser(owner);

			String newLink = "";
			do {
				newLink = UUID.randomUUID().toString().replace("-", "");
			} while (productRepository.findByLink(newLink).isPresent()); // Check if id already exists in DB
			product.setLink(newLink);
		}

		product.setName(name);
		product.setDescription(description);
		product.setPrice(price);
		product.setStock(stock);
		product.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
		Product created = productRepository.save(product);

		return created;
	}

	public boolean deleteProduct(DeleteProductRequest request, String email)
			throws ProductNotFoundException, ApiRequestException {
		Product product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new ProductNotFoundException("Product not found", HttpStatus.NOT_FOUND));

		// Check if user is owner of product
		if (!product.getUser().getEmail().equals(email)) {
			log.error("User not owner of product: " + email);
			throw new ApiRequestException("User not owner of product: " + email, HttpStatus.FORBIDDEN,
					ZonedDateTime.now());
		}

		productRepository.delete(product);
		return true;
	}

	public boolean deleteProductImage(DeleteProductRequest request, String email)
			throws ProductNotFoundException, ApiRequestException {
		Product product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new ProductNotFoundException("Product not found", HttpStatus.NOT_FOUND));

		// Check if user is owner of product
		if (!product.getUser().getEmail().equals(email)) {
			log.error("User not owner of product: " + email);
			throw new ApiRequestException("User not owner of product: " + email, HttpStatus.FORBIDDEN,
					ZonedDateTime.now());
		}

		// Delete image from S3
		String s3Key = product.getS3Key();
		if (s3Key != null && !s3Key.isEmpty()) {
			s3Service.delete(s3Key);
			product.setS3Key(null);
			productRepository.save(product);
		}

		return true;
	}

	public boolean uploadProductImage(String email, long productId, MultipartFile file)
			throws IOException, ApiRequestException {
		Optional<Product> optProduct = productRepository.findById(productId);
		if (optProduct.isEmpty()) {
			return false;
		}

		Product product = optProduct.get();
		// Check if user is owner of product
		if (!product.getUser().getEmail().equals(email)) {
			log.error("User not owner of product: " + email);
			throw new ApiRequestException("User not owner of product: " + email, HttpStatus.FORBIDDEN,
					ZonedDateTime.now());
		}

		// Is there an old product image?
		String oldS3Key = product.getS3Key();

		// Upload new one
		String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename())).replace(" ", "");
		String s3Key = UUID.randomUUID().toString().concat("_").concat(filename);
		byte[] data = file.getBytes();
		s3Service.uploadToS3Bucket(new ByteArrayInputStream(data), s3Key);

		// Save to product
		product.setS3Key(s3Key);
		productRepository.save(product);

		// Delete old one
		if (oldS3Key != null && !oldS3Key.isEmpty()) {
			s3Service.delete(oldS3Key);
		}

		log.info("Uploaded product image from user with email= {}", email);

		return true;
	}

	public Optional<String> getProductImageUrl(long productId) {
		Optional<Product> optProduct = productRepository.findById(productId);
		if (optProduct.isPresent()) {
			String s3Key = optProduct.get().getS3Key();
			if (s3Key != null && !s3Key.isEmpty()) {
				String url = s3Service.presignedURL(s3Key);
				return Optional.ofNullable(url);
			}
		}

		return Optional.empty();
	}
}