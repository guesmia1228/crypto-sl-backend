package com.nefentus.api.Services;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.interfaces.ECKey;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nefentus.api.entities.Role;
import com.nefentus.api.entities.Wallet;
import com.nefentus.api.Errors.UserAlreadyExistsException;
import com.nefentus.api.repositories.WalletRepository;
import com.nefentus.api.repositories.UserRepository;
import com.nefentus.api.entities.User;
import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Errors.WalletNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.web3j.protocol.Web3j;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import com.nefentus.api.security.Encrypt;
import com.nefentus.api.payload.request.SendCryptoRequest;
import com.nefentus.api.payload.response.ParentWalletAddresses;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {
	@Value("${app.name.currencies}")
	private String currenciesStr;

	private final Web3Service web3Service;
	private final WalletRepository walletRepository;
	private final UserRepository userRepository;

	private List<String> currencies() {
		return Arrays.asList(currenciesStr.split(","));
	}

	public List<Wallet> getWallets(User user) {
		List<Wallet> wallets = walletRepository.findByOwner(user);
		return wallets;
	}

	public boolean makeWallets(String username, String password) throws UserNotFoundException {
		// String[] chains = new String[] { "ETH"};
		User user = userRepository.findUserByEmail(username)
				.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));
		List<Wallet> wallets = this.getWallets(user);

		// Check if wallets already exist
		boolean found = false;
		for (Wallet wallet : wallets) {
			if (wallet.getType().equals("ETH")) {
				found = true;
				break;
			}
		}

		if (found) {
			return false;
		} else {
			return web3Service.addWallet(username, password);
		}
	}

	public boolean sendCurrency(User user, SendCryptoRequest request) throws WalletNotFoundException {
		String blockchain = "ETH";
		String walletAddress = null;
		List<Wallet> wallets = this.getWallets(user);

		// Find wallet address
		for (Wallet wallet : wallets) {
			if (wallet.getType().equals(blockchain)) {
				walletAddress = "0x" + wallet.getAddress();
				break;
			}
		}
		if (walletAddress == null) {
			return false;
		}

		// Send currency
		log.info("Sending currency to {} !", request.getToAddress());
		if (request.getTokenAddress() != null) {
			return web3Service.sendToken(request.getTokenAddress(), request.getAmount(), request.getToAddress(),
					walletAddress, request.getPassword());
		} else {
			return web3Service.sendNative(request.getAmount(), request.getToAddress(), walletAddress,
					request.getPassword());
		}
	}
}
