package com.nefentus.api.controller;


import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Services.ClickService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clicks")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
public class ClickController {

    private ClickService clickService;

    @GetMapping("/")
    public ResponseEntity<?> addNewClick(String affLink) {
        try {
            clickService.addClick(affLink);
            return ResponseEntity.ok("Click added.");
        } catch (UserNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
