package com.nefentus.api.controller;

import java.security.Principal;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Services.TransactionService;
import com.nefentus.api.Services.UserService;
import com.nefentus.api.entities.User;
import com.nefentus.api.repositories.UserRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/dashboard/vendor")
@PreAuthorize("hasAnyRole('VENDOR')")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
@Slf4j
public class VendorDashboardController {
	private UserRepository userRepository;
	private ProductService productService;
	UserService userService;
	TransactionService transactionService;

	@GetMapping("/")
	public ResponseEntity<?> checkPermission() {
		log.info("Start check vendor permission! ");
		return ResponseEntity.ok("permission granted!");
	}

	@GetMapping("/income")
	public ResponseEntity<?> getTotalIncome(Principal principal) {
		log.info("Vendor request to get total income! ");
		log.info(principal.getName());

		Optional<User> optUser = userRepository.findUserByEmail(principal.getName());
		if (optUser.isEmpty()) {
			log.error("User not found: " + principal.getName());
			return ResponseEntity.badRequest().body("User not found");
		}

		return ResponseEntity.ok(transactionService.calculateTotalIncomeForUser(optUser.get()));
	}

	@GetMapping("/incomeLast30Days")
	public ResponseEntity<?> getIncomeLast30Days(Principal principal) {
		log.info("Vendor request to get total income per day! ");
		log.info(principal.getName());

		Optional<User> optUser = userRepository.findUserByEmail(principal.getName());
		if (optUser.isEmpty()) {
			log.error("User not found: " + principal.getName());
			return ResponseEntity.badRequest().body("User not found");
		}

		return ResponseEntity.ok(transactionService.calculateIncomeForUserLast30Days(optUser.get()));
	}

}
