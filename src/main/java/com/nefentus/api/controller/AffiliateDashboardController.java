package com.nefentus.api.controller;

import com.nefentus.api.Errors.UserAlreadyExistsException;
import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Services.ClickService;
import com.nefentus.api.Services.TransactionService;
import com.nefentus.api.Services.UserService;
import com.nefentus.api.entities.AffiliateCounter;
import com.nefentus.api.payload.request.AddUserRequest;
import com.nefentus.api.payload.response.DashboardDataResponse;
import com.nefentus.api.payload.response.MessageResponse;
import com.nefentus.api.repositories.AffiliateCounterRepository;
import com.nefentus.api.repositories.AffiliateRepository;
import com.nefentus.api.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/dashboard/affiliate")
@PreAuthorize("hasRole('AFFILIATE')")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
public class AffiliateDashboardController {
    UserService userService;
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

    //get all clicks by the aff links below him
    @GetMapping("/clicks")
    public ResponseEntity<?> getClicks(Principal principal){
        return ResponseEntity.ok(clickService.calculateTotalClicks(principal.getName()));
    }
}
