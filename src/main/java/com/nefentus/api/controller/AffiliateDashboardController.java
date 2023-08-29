package com.nefentus.api.controller;

import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Services.ClickService;
import com.nefentus.api.Services.TransactionService;
import com.nefentus.api.Services.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/dashboard/affiliate")
@PreAuthorize("hasRole('AFFILIATE')")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
@Slf4j
public class AffiliateDashboardController {
	UserService userService;
	TransactionService transactionService;
	ClickService clickService;

	@GetMapping("/")
	public ResponseEntity<?> checkPermission() {
		log.info("Start check Affiliate permission! ");
		return ResponseEntity.ok("permission granted!");
	}

	// get income for users below him and calculate the percentage
	@GetMapping("/income")
	public ResponseEntity<?> getTotalIncome(Principal principal) throws UserNotFoundException {
		log.info("Affiliate request to get total income! ");
		return ResponseEntity.ok(transactionService.calculateTotalIncome(principal.getName()));
	}

	// same as above but per day
	@GetMapping("/totalIncomesPerDay")
	public ResponseEntity<?> getTotalIncomesPerDay(Principal principal) throws UserNotFoundException {
		log.info("Affiliate request to get total income per day! ");
		return ResponseEntity.ok(transactionService.getTotalPriceByDay(principal.getName()));
	}

	// get all users below him in count
	@GetMapping("/usersCount")
	public ResponseEntity<?> getTotalUserCount(Principal principal) {
		log.info("Affiliate request to count total users! ");
		return ResponseEntity.ok(userService.calculateTotalClicks(principal.getName()));
	}

	// get all clicks by the aff links below him
	@GetMapping("/clicks")
	public ResponseEntity<?> getClicks(Principal principal) {
		log.info("Affiliate request to count total clicks! ");
		return ResponseEntity.ok(clickService.calculateTotalClicks(principal.getName()));
	}
}
