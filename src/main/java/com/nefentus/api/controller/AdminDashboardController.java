package com.nefentus.api.controller;

import com.nefentus.api.Errors.UserAlreadyExistsException;
import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Services.ClickService;
import com.nefentus.api.Services.TransactionService;
import com.nefentus.api.Services.UserService;
import com.nefentus.api.payload.request.AddUserRequest;
import com.nefentus.api.payload.request.ChangeUserStateRequest;
import com.nefentus.api.repositories.AffiliateCounterRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
@Slf4j
public class AdminDashboardController {
	UserService userService;
	AffiliateCounterRepository affiliateCounterRepository;
	TransactionService transactionService;
	ClickService clickService;

	@GetMapping("/")
	public ResponseEntity<?> checkPermission() {
		log.info("Start check admin permission! ");
		return ResponseEntity.ok("permission granted!");
	}

	@GetMapping("/income")
	public ResponseEntity<?> getTotalIncome() {
		log.info("Admin request to get total income! ");
		return ResponseEntity.ok(transactionService.calculateTotalIncome());
	}

	@GetMapping("/totalIncomesPerDay")
	public ResponseEntity<?> getTotalIncomesPerDay() {
		log.info("Admin request to get total income per day! ");
		return ResponseEntity.ok(transactionService.getTotalPriceByDay());
	}

	@GetMapping("/usersCount")
	public ResponseEntity<?> getTotalUserCount() {
		log.info("Admin request to calculate total users!");
		return ResponseEntity.ok(userService.calculateTotalClicks());
	}

	@GetMapping("/users")
	public ResponseEntity<?> getAllUsers() {
		log.info("Admin query all users! ");
		return ResponseEntity.ok(userService.getAllUsers());
	}

	@GetMapping("/clicks")
	public ResponseEntity<?> getClicks() {
		log.info("Admin request to calculate total clicks!");
		return ResponseEntity.ok(clickService.calculateTotalClicks());
	}

	@PostMapping("/users")
	public ResponseEntity<?> addUser(@RequestBody AddUserRequest addUserRequest) throws UserAlreadyExistsException {
		log.info("Admin process to add new user! ");
		return ResponseEntity.ok(userService.addUser(addUserRequest));
	}

	@PatchMapping("/users")
	public ResponseEntity<?> changeState(@RequestBody ChangeUserStateRequest changeUserStateRequest)
			throws UserNotFoundException {
		log.info("Admin request to change state of user !");
		return ResponseEntity.ok(userService.changeUserState(changeUserStateRequest));
	}

	@GetMapping("/userroles")
	public ResponseEntity<?> getRoles() {
		log.info("Admin request to get roles!");
		return ResponseEntity.ok(userService.getRolesStatus());
	}
}
