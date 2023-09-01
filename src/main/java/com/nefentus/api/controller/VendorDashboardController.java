package com.nefentus.api.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

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

import com.nefentus.api.Errors.ApiRequestException;
import com.nefentus.api.Errors.ProductNotFoundException;
import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Services.TransactionService;
import com.nefentus.api.Services.UserService;
import com.nefentus.api.Services.ProductService;
import com.nefentus.api.Services.InvoiceService;
import com.nefentus.api.entities.User;
import com.nefentus.api.payload.request.UpsertProductRequest;
import com.nefentus.api.payload.response.DashboardNumberResponse;
import com.nefentus.api.payload.response.MessageResponse;
import com.nefentus.api.payload.request.CreateInvoiceRequest;
import com.nefentus.api.payload.request.DeleteProductRequest;
import com.nefentus.api.repositories.UserRepository;
import com.nefentus.api.repositories.OrderRepository;
import com.nefentus.api.repositories.InvoiceRepository;
import com.nefentus.api.entities.Product;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/dashboard/vendor")
@PreAuthorize("hasAnyRole('VENDOR','AFFILIATE','BROKER','SENIOR_BROKER','LEADER','ADMIN')")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
@Slf4j
public class VendorDashboardController {
	private UserRepository userRepository;
	private OrderRepository orderRepository;
	private InvoiceRepository invoiceRepository;
	private ProductService productService;
	UserService userService;
	TransactionService transactionService;
	InvoiceService invoiceService;

	@GetMapping("/")
	public ResponseEntity<?> checkPermission() {
		log.info("Start check vendor permission! ");
		return ResponseEntity.ok("permission granted!");
	}

	@GetMapping("/income")
	public ResponseEntity<?> getTotalIncome(Principal principal) {
		log.info("Vendor request to get total income! ");
		log.info(principal.getName());

		try {
			Map<String, DashboardNumberResponse> result = new HashMap<>();
			result.put("total", transactionService.calculateTotalIncome(principal.getName()));
			result.put("last30Days", transactionService.calculateIncomeLast30Days(principal.getName()));
			result.put("last24Hours", transactionService.calculateIncomeLast24Hours(principal.getName()));
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body("User not found");
		}
	}

	@GetMapping("/numOrders")
	public ResponseEntity<?> getNumOrders(Principal principal) {
		log.info("Vendor request to get total income! ");
		log.info(principal.getName());

		try {
			return ResponseEntity.ok(transactionService.getNumberOfOrders(principal.getName()));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body("User not found");
		}
	}

	@GetMapping("/totalIncomesPerDay")
	public ResponseEntity<?> getTotalIncomesPerDay(Principal principal) {
		log.info("Admin request to get total income per day! ");
		try {
			return ResponseEntity.ok(transactionService.getTotalPriceByDayAsVendor(principal.getName()));
		} catch (UserNotFoundException e) {
			log.error("User not found: " + principal.getName());
			return ResponseEntity.badRequest().body("User not found");
		}
	}

	@GetMapping("/products")
	public ResponseEntity<?> getProducts(Principal principal) {
		log.info("Vendor get products");
		try {
			return ResponseEntity.ok(productService.getProducts(principal.getName()));
		} catch (UserNotFoundException e) {
			log.error("User not found: " + principal.getName());
			return ResponseEntity.badRequest().body("User not found");
		}
	}

	@PostMapping("/products/upsert")
	public ResponseEntity<?> upsertImage(@RequestBody UpsertProductRequest upsertRequest,
			Principal principal) throws IOException {
		log.info("Vendor upserts product");
		try {
			Product product = productService.upsertProduct(upsertRequest.getProductId(), upsertRequest.getName(),
					upsertRequest.getDescription(), upsertRequest.getPrice(), upsertRequest.getStock(),
					principal.getName());
			return ResponseEntity.ok().body(product);
		} catch (ApiRequestException e) {
			log.error("User is not owner of the product: " + principal.getName());
			return ResponseEntity.badRequest().body("User is not owner of the product");
		} catch (UserNotFoundException e) {
			log.error("User not found: " + principal.getName());
			return ResponseEntity.badRequest().body("User not found");
		}
	}

	@PostMapping("/products/delete")
	public ResponseEntity<?> deleteProduct(@RequestBody DeleteProductRequest deleteProductRequest,
			Principal principal) {
		log.info("Vendor delete product! ");

		try {
			return ResponseEntity.ok(productService.deleteProduct(deleteProductRequest, principal.getName()));
		} catch (ProductNotFoundException e) {
			log.error("Product not found: " + principal.getName());
			return ResponseEntity.badRequest().body("Product not found");
		} catch (ApiRequestException e) {
			log.error("User is not owner of the product: " + principal.getName());
			return ResponseEntity.badRequest().body("User is not owner of the product");
		}
	}

	@PostMapping("/products/deleteImage")
	public ResponseEntity<?> deleteProductImage(@RequestBody DeleteProductRequest deleteProductRequest,
			Principal principal) {
		log.info("Vendor delete product image! ");

		try {
			return ResponseEntity.ok(productService.deleteProductImage(deleteProductRequest, principal.getName()));
		} catch (ProductNotFoundException e) {
			log.error("Product not found: " + principal.getName());
			return ResponseEntity.badRequest().body("Product not found");
		} catch (ApiRequestException e) {
			log.error("User is not owner of the product: " + principal.getName());
			return ResponseEntity.badRequest().body("User is not owner of the product");
		}
	}

	@PostMapping("/products/uploadImage")
	public ResponseEntity<?> uploadProductImage(@RequestPart("file") MultipartFile file,
			@RequestParam("productId") long productId,
			Principal principal) throws IOException {
		log.info("Vendor uploads product image");
		try {
			productService.uploadProductImage(principal.getName(), productId, file);
			return ResponseEntity.ok(new MessageResponse("successfull!"));
		} catch (ApiRequestException e) {
			log.error("User is not owner of the product: " + principal.getName());
			return ResponseEntity.badRequest().body("User is not owner of the product");
		}
	}

	@GetMapping("/products/image/{productId}")
	public ResponseEntity<?> getProductImage(@PathVariable long productId) {
		log.info("Vendor get product image");
		return ResponseEntity.ok(productService.getProductImageUrl(productId));
	}

	@GetMapping("/orders")
	public ResponseEntity<?> getOrders(Principal principal) {
		log.info("Vendor get orders");
		return ResponseEntity.ok(orderRepository.findAllBySellerEmail(principal.getName()));
	}

	@GetMapping("/invoices")
	public ResponseEntity<?> getInvoices(Principal principal) {
		log.info("Vendor get invoices");
		Optional<User> optUser = userRepository.findUserByEmail(principal.getName());
		if (optUser.isEmpty()) {
			return ResponseEntity.badRequest().body("User not found");
		}

		return ResponseEntity.ok(invoiceRepository.findByUser(optUser.get()));
	}

	@PostMapping("/invoice")
	public ResponseEntity<?> createInvoice(@RequestBody CreateInvoiceRequest request,
			Principal principal) {
		log.info("Create an invoice");
		try {
			return ResponseEntity.ok(invoiceService.createInvoice(request, principal.getName()));
		} catch (NoSuchElementException e) {
			return ResponseEntity.badRequest().build();
		}
	}
}
