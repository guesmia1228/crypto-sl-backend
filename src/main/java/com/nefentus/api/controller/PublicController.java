package com.nefentus.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nefentus.api.Services.ProductService;
import com.nefentus.api.Services.TransactionService;
import com.nefentus.api.Services.UserService;
import com.nefentus.api.repositories.ProductRepository;
import com.nefentus.api.repositories.UserRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
@Slf4j
public class PublicController {
	private ProductService productService;
	@Autowired
	private UserService userService;

	@GetMapping("/product/{productLink}")
	public ResponseEntity<?> getProduct(@PathVariable String productLink) {
		log.info("Get product link");
		return ResponseEntity.ok(productService.getProductByLink(productLink));
	}

	@GetMapping("/productImage/{productLink}")
	public ResponseEntity<?> getProductImage(@PathVariable String productLink) {
		log.info("Get product image");
		return ResponseEntity.ok(productService.getProductImageByLink(productLink));
	}

	@GetMapping("/hierarchy/{userId}")
	public ResponseEntity<?> getHierarchy(@PathVariable long userId) {
		log.info("Get hierarchy");
		return ResponseEntity.ok(userService.getParentWalletAddresses(userId));
	}
}
