package com.nefentus.api.controller;

import com.nefentus.api.Errors.UserAlreadyExistsException;
import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Services.ClickService;
import com.nefentus.api.Services.TransactionService;
import com.nefentus.api.Services.UserService;
import com.nefentus.api.payload.request.AddUserRequest;
import com.nefentus.api.payload.request.ChangeUserStateRequest;
import com.nefentus.api.payload.response.UserDisplayAdminResponse;
import com.nefentus.api.repositories.UserRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

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
	UserRepository userRepository;
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
		return ResponseEntity.ok(userService.calculateRegistrations());
	}

	@GetMapping("/users")
	public ResponseEntity<?> getAllUsers() {
		log.info("Admin query all users! ");
		List<UserDisplayAdminResponse> userData = userRepository.findAll().stream()
				.map(user -> {
					UserDisplayAdminResponse response = UserDisplayAdminResponse.fromUser(user);
					response.setIncome(transactionService.getIncomeForUser(user));
					return response;
				})
				.collect(Collectors.toList());
		return ResponseEntity.ok(userData);
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
		return ResponseEntity.ok(userService.activateUserByEmail(changeUserStateRequest.getUseremail()));
	}

	@GetMapping("/userroles")
	public ResponseEntity<?> getRoles() {
		log.info("Admin request to get roles!");
		return ResponseEntity.ok(userService.getRolesStatus());
	}

	@GetMapping("/numOrders")
	public ResponseEntity<?> getNumOrders() {
		log.info("Vendor request to get total income! ");

		try {
			return ResponseEntity.ok(transactionService.getNumberOfOrders());
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body("User not found");
		}
	}

	@GetMapping("/users/delete/{email}")
	public ResponseEntity<?> deleteUser(@PathVariable String email) {
		log.info("Delete a user");
		try {
			return ResponseEntity.ok(userService.deleteUser(email));
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/users/deactivate/{email}")
	public ResponseEntity<?> deactivateUser(@PathVariable String email) {
		log.info("Activate a user");
		try {
			return ResponseEntity.ok(userService.deactivateUserByEmail(email));
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}
}
