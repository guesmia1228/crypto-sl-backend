package com.nefentus.api.controller;

import java.security.Principal;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nefentus.api.Errors.InsufficientFundsException;
import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Errors.WalletNotFoundException;
import com.nefentus.api.Services.WalletService;
import com.nefentus.api.entities.User;
import com.nefentus.api.repositories.UserRepository;
import com.nefentus.api.repositories.WalletRepository;

import com.nefentus.api.entities.Wallet;
import com.nefentus.api.payload.request.SendCryptoRequest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/wallet")
@PreAuthorize("isAuthenticated()")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
@Slf4j
public class WalletController {
	private UserRepository userRepository;
	private WalletService walletService;

	@GetMapping("/addresses")
	public ResponseEntity<?> getWallets(Principal principal) {
		String username = principal.getName();
		log.info("Start getting wallets for {} !", username);

		try {
			User user = userRepository.findUserByEmail(username)
					.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));
			List<Wallet> wallets = walletService.getWallets(user);
			List<Map<String, Object>> addresses = wallets.stream().map(wallet -> {
				Map<String, Object> addressInfo = new HashMap<String, Object>();
				addressInfo.put("address", wallet.getAddress());
				addressInfo.put("type", wallet.getType());
				addressInfo.put("internal", wallet.getPrivateKey() != null);
				return addressInfo;
			}).toList();
			return ResponseEntity.ok(addresses);
		} catch (UserNotFoundException e) {
			log.error("User not found: " + username);
			return ResponseEntity.badRequest().body("User not found");
		}
	}

	@PostMapping("/send")
	public ResponseEntity<?> sendCurrency(@RequestBody SendCryptoRequest request, Principal principal) {
		String username = principal.getName();
		log.info("Start getting wallets for {} !", username);

		try {
			User user = userRepository.findUserByEmail(username)
					.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));
			boolean success = walletService.sendCurrency(user, request);
			return ResponseEntity.ok(success);
		} catch (UserNotFoundException e) {
			log.error("User not found: " + username);
			return ResponseEntity.badRequest().body("User not found");
		} catch (WalletNotFoundException e) {
			log.error("Wallet not found: " + e.getMessage());
			return ResponseEntity.badRequest().body("Wallet not found");
		}
	}

	@GetMapping("/address/{address}")
	public ResponseEntity<?> registerAddress(@PathVariable String address, Principal principal) {
		log.info("Register a new address");
		try {
			Wallet wallet = walletService.addWalletWithAddress(address, principal.getName());
			return ResponseEntity.ok(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResponseEntity.badRequest().build();
	}
}
