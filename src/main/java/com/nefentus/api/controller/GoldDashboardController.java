package com.nefentus.api.controller;

import com.nefentus.api.Errors.UserAlreadyExistsException;
import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Services.ClickService;
import com.nefentus.api.Services.TransactionService;
import com.nefentus.api.Services.UserService;
import com.nefentus.api.payload.request.AddUserRequest;
import com.nefentus.api.payload.response.MessageResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/dashboard/gold")
@PreAuthorize("hasRole('GOLD_PARTNER')")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
@Slf4j
public class GoldDashboardController {
    UserService userService;
    TransactionService transactionService;
    ClickService clickService;

    @GetMapping("/")
    public ResponseEntity<?> checkPermission() {
        log.info("Check Gold user permission! ");
        return ResponseEntity.ok("permission granted!");
    }

    //get income for users below him and calculate the percentage
    @GetMapping("/income")
    public ResponseEntity<?> getTotalIncome(Principal principal) throws UserNotFoundException{
        log.info("Gold user request to get total income with email= {}", principal.getName());
        return ResponseEntity.ok(transactionService.calculateTotalIncome(principal.getName()));
    }

    //same as above but per day
    @GetMapping("/totalIncomesPerDay")
    public ResponseEntity<?> getTotalIncomesPerDay(Principal principal) throws UserNotFoundException{
        log.info("Gold user request to get total income per day with email= {}", principal.getName());
        return ResponseEntity.ok(transactionService.getTotalPriceByDay(principal.getName()));
    }

    //get all users below him in count
    @GetMapping("/usersCount")
    public ResponseEntity<?> getTotalUserCount(Principal principal) {
        log.info("Gold user request to count total users with email= {}", principal.getName());
        return ResponseEntity.ok(userService.calculateTotalClicks(principal.getName()));
    }

    //get all clicks by the aff links below him
    @GetMapping("/clicks")
    public ResponseEntity<?> getClicks(Principal principal) {
        log.info("Gold user request to caculate total clicks with email= {}", principal.getName());
        return ResponseEntity.ok(clickService.calculateTotalClicks(principal.getName()));
    }

    //create a new user below him
    @PostMapping("/users")
    public ResponseEntity<?> getClicks(@RequestBody AddUserRequest addUserRequest, Principal principal) throws UserAlreadyExistsException {
        log.info("Gold user= {} request to add new user! ", principal.getName());
        return ResponseEntity.ok(userService.addUser(addUserRequest, principal.getName()));
    }

    @GetMapping("/userroles")
    public ResponseEntity<?> getRoles(Principal principal) {
        return ResponseEntity.ok(userService.getRolesStatus(principal.getName()));
    }

}
