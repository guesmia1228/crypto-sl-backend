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

import java.security.Principal;

@RestController
@RequestMapping("/api/dashboard/diamond")
@PreAuthorize("hasRole('DIAMOND_PARTNER')")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
public class DiamondDashboardController {
    UserService userService;
    AffiliateCounterRepository affiliateCounterRepository;
    TransactionService transactionService;
    ClickService clickService;

    @GetMapping("/")
    public ResponseEntity<?> checkPermission(){
        return ResponseEntity.ok("permission granted!");
    }

    //get income for users below him and calculate the percentage
    @GetMapping("/income")
    public ResponseEntity<?> getTotalIncome(Principal principal){
        try {
            return ResponseEntity.ok(transactionService.calculateTotalIncome(principal.getName()));
        } catch (UserNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //same as above but per day
    @GetMapping("/totalIncomesPerDay")
    public ResponseEntity<?> getTotalIncomesPerDay(Principal principal){
        try {
            return ResponseEntity.ok(transactionService.getTotalPriceByDay(principal.getName()));
        } catch (UserNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //get all users below him in count
    @GetMapping("/usersCount")
    public ResponseEntity<?> getTotalUserCount(Principal principal){
        return ResponseEntity.ok(userService.calculateTotalClicks(principal.getName()));
    }

    //get all users below him in a list
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Principal principal){
        return ResponseEntity.ok(userService.getAllUsers(principal.getName()));
    }

    //get all clicks by the aff links below him
    @GetMapping("/clicks")
    public ResponseEntity<?> getClicks(Principal principal){
        return ResponseEntity.ok(clickService.calculateTotalClicks(principal.getName()));
    }

    //create a new user below him
    @PostMapping("/users")
    public ResponseEntity<?> getClicks(@RequestBody AddUserRequest addUserRequest, Principal principal){
        try {
            return ResponseEntity.ok(userService.addUser(addUserRequest, principal.getName()));
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    //STILL TODO GET USER ROLES
    @GetMapping("/userroles")
    public ResponseEntity<?> getRoles(Principal principal){
        return ResponseEntity.ok(userService.getRolesStatus(principal.getName()));
    }

}
