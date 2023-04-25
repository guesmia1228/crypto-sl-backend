package com.nefentus.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/vendor")
@PreAuthorize("hasAnyRole('VENDOR')")
@CrossOrigin(origins = "*", maxAge = 3600)
public class VendorDashboardController {


    //TODO READ DATA
    @GetMapping()
    public ResponseEntity<?> authorize(){
        return ResponseEntity.ok("successfully authorized!");
    }

}
