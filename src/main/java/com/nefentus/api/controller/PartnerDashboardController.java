package com.nefentus.api.controller;

import com.nefentus.api.Errors.BadRequestException;
import com.nefentus.api.Errors.UserAlreadyExistsException;
import com.nefentus.api.Errors.UserFoundException;
import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Services.ClickService;
import com.nefentus.api.Services.TransactionService;
import com.nefentus.api.Services.UserService;
import com.nefentus.api.entities.User;
import com.nefentus.api.payload.request.AddUserRequest;
import com.nefentus.api.payload.response.MessageResponse;
import com.nefentus.api.payload.response.UserDisplayAdminResponse;
import com.nefentus.api.repositories.HierarchyRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard/partner")
@PreAuthorize("hasAnyRole('AFFILIATE','BROKER','SENIOR_BROKER','LEADER')")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
@Slf4j
public class PartnerDashboardController {
	UserService userService;
	HierarchyRepository hierarchyRepository;
	TransactionService transactionService;
	ClickService clickService;

	@GetMapping("/")
	public ResponseEntity<?> checkPermission() {
		log.info("Check Gold user permission! ");
		return ResponseEntity.ok("permission granted!");
	}

	// get income for users below him and calculate the percentage
	@GetMapping("/income")
	public ResponseEntity<?> getTotalIncome(Principal principal) throws UserNotFoundException {
		log.info("Gold user request to get total income with email= {}", principal.getName());
		return ResponseEntity.ok(transactionService.calculateTotalIncome(principal.getName()));
	}

	// same as above but per day
	@GetMapping("/totalIncomesPerDay")
	public ResponseEntity<?> getTotalIncomesPerDay(Principal principal) throws UserNotFoundException {
		log.info("Gold user request to get total income per day with email= {}", principal.getName());
		return ResponseEntity.ok(transactionService.getTotalPriceByDay(principal.getName()));
	}

	// get all users below him in count
	@GetMapping("/usersCount")
	public ResponseEntity<?> getTotalUserCount(Principal principal) {
		log.info("Gold user request to count total users with email= {}", principal.getName());
		return ResponseEntity.ok(userService.calculateRegistrations(principal.getName()));
	}

	// get all clicks by the aff links below him
	@GetMapping("/clicks")
	public ResponseEntity<?> getClicks(Principal principal) {
		log.info("Gold user request to caculate total clicks with email= {}", principal.getName());
		return ResponseEntity.ok(clickService.calculateTotalClicks(principal.getName()));
	}

	// create a new user below him
	@PostMapping("/users")
	public ResponseEntity<?> getClicks(@RequestBody AddUserRequest addUserRequest, Principal principal)
			throws UserAlreadyExistsException, BadRequestException {
		log.info("Gold user= {} request to add new user! ", principal.getName());
		return ResponseEntity.ok(userService.addUser(addUserRequest, principal.getName()));
	}

	@PutMapping("/users")
	public ResponseEntity<?> updateUser(@RequestBody AddUserRequest addUserRequest, Principal principal)
			throws UserFoundException, UserNotFoundException, BadRequestException {
		log.info("Admin process to update user! ");
		return ResponseEntity.ok(userService.updateUserAdmin(addUserRequest, principal.getName()));
	}

	// get all users below him in a list
	@GetMapping("/users")
	public ResponseEntity<?> getAllUsers(Principal principal) {
		log.info("Diamond user request to get all users with email= {}", principal.getName());
		List<User> users = userService.getChildrenRecursive(principal.getName());
		List<UserDisplayAdminResponse> userData = users.stream()
				.map(user -> {
					UserDisplayAdminResponse response = UserDisplayAdminResponse.fromUser(user);
					response.setIncome(transactionService.getIncomeForUser(user));
					return response;
				})
				.collect(Collectors.toList());
		return ResponseEntity.ok(userData);
	}

	@GetMapping("/userroles")
	public ResponseEntity<?> getRoles(Principal principal) {
		return ResponseEntity.ok(userService.getRolesStatus(principal.getName()));
	}

	@GetMapping("/numOrders")
	public ResponseEntity<?> getNumOrders(Principal principal) {
		log.info("Vendor request to get number of order for user= {} ", principal.getName());

		try {
			return ResponseEntity.ok(transactionService.getNumberOfOrders(principal.getName()));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body("User not found");
		}
	}
}
