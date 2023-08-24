package com.nefentus.api.controller;

import java.security.Principal;

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
import com.nefentus.api.payload.request.AddOrderRequest;
import com.nefentus.api.payload.request.SendCryptoRequest;
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
	private TransactionService transactionService;
	private InvoiceService invoiceService;
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

	@PostMapping("/transaction")
	public ResponseEntity<?> newTransaction(@RequestBody AddOrderRequest request, Principal principal) {
		// TODO: How to check validity of the request?
		log.info("Add a new transaction");
		try {
			return ResponseEntity.ok(transactionService.addTransaction(request));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResponseEntity.badRequest().build();
	}

	@GetMapping("/invoice/{invoiceLink}")
	public ResponseEntity<?> getHierarchy(@PathVariable String invoiceLink) {
		log.info("Get invoice");
		return ResponseEntity.ok(invoiceService.getInvoice(invoiceLink));
	}
}
