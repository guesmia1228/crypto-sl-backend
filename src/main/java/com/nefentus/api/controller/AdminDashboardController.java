package com.nefentus.api.controller;


import com.nefentus.api.Errors.UserAlreadyExistsException;
import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Services.ClickService;
import com.nefentus.api.Services.TransactionService;
import com.nefentus.api.Services.UserService;
import com.nefentus.api.payload.request.AddUserRequest;
import com.nefentus.api.payload.request.ChangeUserStateRequest;
import com.nefentus.api.payload.response.MessageResponse;
import com.nefentus.api.repositories.AffiliateCounterRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
public class AdminDashboardController {
    UserService userService;
    AffiliateCounterRepository affiliateCounterRepository;
    TransactionService transactionService;
    ClickService clickService;

    @GetMapping("/")
    public ResponseEntity<?> checkPermission() {
        return ResponseEntity.ok("permission granted!");
    }

    @GetMapping("/income")
    public ResponseEntity<?> getTotalIncome() {
        return ResponseEntity.ok(transactionService.calculateTotalIncome());
    }

    @GetMapping("/totalIncomesPerDay")
    public ResponseEntity<?> getTotalIncomesPerDay() {
        return ResponseEntity.ok(transactionService.getTotalPriceByDay());
    }

    @GetMapping("/usersCount")
    public ResponseEntity<?> getTotalUserCount() {
        return ResponseEntity.ok(userService.calculateTotalClicks());
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/clicks")
    public ResponseEntity<?> getClicks() {
        return ResponseEntity.ok(clickService.calculateTotalClicks());
    }

    @PostMapping("/users")
    public ResponseEntity<?> getClicks(@RequestBody AddUserRequest addUserRequest) {
        try {
            return ResponseEntity.ok(userService.addUser(addUserRequest));
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PatchMapping("/users")
    public ResponseEntity<?> changeState(@RequestBody ChangeUserStateRequest changeUserStateRequest) {
        try {
            return ResponseEntity.ok(userService.changeUserState(changeUserStateRequest));
        } catch (UserNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/userroles")
    public ResponseEntity<?> getRoles() {
        return ResponseEntity.ok(userService.getRolesStatus());
    }
}
