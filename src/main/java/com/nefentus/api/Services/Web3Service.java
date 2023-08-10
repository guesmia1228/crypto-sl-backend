package com.nefentus.api.Services;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECKey;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

import org.web3j.protocol.core.methods.response.*;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.crypto.Credentials;
import org.web3j.tx.Transfer;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;
import org.web3j.utils.Convert;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.lang.InterruptedException;

import com.nefentus.api.security.Encrypt;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class Web3Service {
	@Autowired
	private Web3j web3j;

	private final WalletRepository walletRepository;
	private final UserRepository userRepository;

	public final String UNISWAP_ROUTER = "0xE592427A0AEce92De3Edee1F18E0157C05861564";
	public final String WETH_ADDRESS = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2";
	public final String USDC_ADDRESS = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48";

	public Map<String, Object[]> currencyToBlockchain() {
		Map<String, Object[]> map = new HashMap<String, Object[]>();
		// Blockchain, TokenAddress, TokenDecimals
		map.put("ETH", new Object[] { "ETH", "0x0", 18 });
		map.put("USDT", new Object[] { "ETH", "0xdAC17F958D2ee523a2206206994597C13D831ec7", 6 });
		map.put("USDC", new Object[] { "ETH", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", 6 });
		return map;
	}

	public boolean sendNative(String amount, String toAddress, String walletAddress, String password)
			throws WalletNotFoundException {
		String walletAddressWithoutPrefix = walletAddress.substring(2);
		ECKeyPair keyPair = this.keysFromWallet(password, walletAddressWithoutPrefix);
		if (keyPair == null) {
			log.error("Error getting keys from wallet");
			return false;
		}

		// Send transaction
		Credentials credentials = Credentials.create(keyPair);
		TransactionReceipt transactionReceipt = null;

		int tries = 0;
		while (tries < 3) {
			try {
				transactionReceipt = Transfer
						.sendFunds(web3j, credentials, toAddress, new BigDecimal(amount), Convert.Unit.ETHER)
						.sendAsync().get();
				break;
			} catch (Exception e) {
				log.warn("Error calling smart contract (transfer): {}", e.getMessage());
			}

			tries += 1;
		}

		if (transactionReceipt != null) {
			return true;
		} else {
			return false;
		}
	}

	public boolean sendToken(String tokenAddress, String amount, String toAddress, String walletAddress,
			String password) throws WalletNotFoundException {
		// Find the currency
		int tokenDecimals = -1;
		Map<String, Object[]> currencyToBlockchain = this.currencyToBlockchain();
		for (String currency : currencyToBlockchain.keySet()) {
			Object[] values = currencyToBlockchain.get(currency);
			if (values[1].equals(tokenAddress)) {
				tokenDecimals = (int) values[2];
			}
		}

		if (tokenDecimals == -1) {
			log.error("Token not found: {}", tokenAddress);
			return false;
		}

		// Get key pair
		String walletAddressWithoutPrefix = walletAddress.substring(2);
		ECKeyPair keyPair = this.keysFromWallet(password, walletAddressWithoutPrefix);
		Credentials credentials = Credentials.create(keyPair);

		// Make input parameters
		List<Type> inputParameters = new ArrayList<>();
		inputParameters.add(new Address(toAddress));
		BigDecimal amountInWei = new BigDecimal(amount).multiply(BigDecimal.valueOf(10).pow(tokenDecimals));
		inputParameters.add(new Uint256(amountInWei.toBigInteger()));

		final Function function = new Function("transfer", inputParameters, Collections.emptyList());
		String encodedFunction = FunctionEncoder.encode(function);

		TransactionReceipt transactionReceipt = null;
		int tries = 0;
		while (tries < 1) {
			try {
				// Get the nonce
				EthGetTransactionCount ethGetTransactionCount = web3j
						.ethGetTransactionCount(walletAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
				BigInteger nonce = ethGetTransactionCount.getTransactionCount();

				// Create the transaction
				RawTransactionManager rawTransactionManager = new RawTransactionManager(web3j, credentials, (long) 1);
				EthSendTransaction response = rawTransactionManager.sendTransaction(
						Convert.toWei("20", Convert.Unit.GWEI).toBigInteger(),
						new BigInteger("100000"), tokenAddress, encodedFunction, BigInteger.ZERO);

				// Get the transaction hash
				String transactionHash = response.getTransactionHash();
				log.info("Transaction hash: {}", transactionHash);

				if (transactionHash != null) {
					// Try for 5 minutes at most
					int tries2 = 0;
					while (tries2 < 150) {
						EthGetTransactionReceipt getTransactionReceipt = web3j.ethGetTransactionReceipt(transactionHash)
								.sendAsync().get();
						if (getTransactionReceipt.getTransactionReceipt().isPresent()) {
							transactionReceipt = getTransactionReceipt.getTransactionReceipt().get();
							break;
						} else {
							Thread.sleep(2000);
							tries2++;
						}
					}

					if (transactionReceipt != null) {
						break;
					}
				} else {
					log.warn("Error getting transaction receipt (transfer): {}", response.getError().getMessage());
				}

			} catch (Exception e) {
				log.warn("Error calling smart contract (transfer): {}", e.getMessage());
			}

			tries += 1;
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				log.error("Error sleeping: {}", e.getMessage());
			}
		}

		if (transactionReceipt != null) {
			BigInteger status = Numeric.parsePaddedNumberHex(transactionReceipt.getStatus());
			return status.equals(BigInteger.valueOf(0));
		} else {
			return false;
		}
	}

	public boolean addWallet(String username, String password) {
		String blockchain = "ETH";

		User owner;
		try {
			owner = userRepository.findUserByEmail(username)
					.orElseThrow(() -> new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST));
		} catch (UserNotFoundException e) {
			log.error("User not found: " + username);
			return false;
		}

		// 1. Create a new KeyPair
		ECKeyPair keyPair = null;
		try {
			keyPair = Keys.createEcKeyPair();
		} catch (Exception e) {
			log.error("Error creating key pair: {}", e.getMessage());
			return false;
		}
		String publicKey = Numeric.toHexStringWithPrefix(keyPair.getPublicKey());
		String address = Keys.getAddress(keyPair.getPublicKey());

		// 2. Create a new Wallet that is saved in the database
		Wallet wallet = new Wallet();
		wallet.setOwner(owner);
		wallet.setAddress(address);
		wallet.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
		wallet.setType("ETH");

		// 3. Encrypt private key with password
		String salt = address + blockchain;
		SecretKey secretkey = null;
		try {
			secretkey = Encrypt.getKeyFromPassword(password, salt);
		} catch (Exception e) {
			log.error("Error creating secret key: {}", e.getMessage());
			return false;
		}
		String encryptedPrivateKey = null;
		GCMParameterSpec nonce = Encrypt.generateNonce();
		try {
			encryptedPrivateKey = Encrypt.encrypt(keyPair.getPrivateKey().toString(), secretkey, nonce);
		} catch (Exception e) {
			log.error("Error encrypting private key: {}", e.getMessage());
			return false;
		}

		// 4. Save wallet
		wallet.setPrivateKey(encryptedPrivateKey);
		wallet.setNonce(nonce.getIV());

		Wallet created = walletRepository.save(wallet);

		log.info("Created new wallet for user with email {}", username);

		// 5. Check decrypting
		ECKeyPair keys2 = null;
		try {
			keys2 = this.keysFromWallet(password, address);
		} catch (WalletNotFoundException e) {
			log.error("Error getting keys from wallet: {}", e.getMessage());
			return false;
		}
		if (!keys2.getPrivateKey().toString().equals(keyPair.getPrivateKey().toString())) {
			log.error("Keys do not match!");
			return false;
		}

		return true;
	}

	public ECKeyPair keysFromWallet(String password, String address) throws WalletNotFoundException {
		String blockchain = "ETH";

		Optional<Wallet> optWwallet = walletRepository.findByAddress(address);
		if (optWwallet.isEmpty()) {
			log.error("Wallet not found: " + address);
			throw new WalletNotFoundException("Wallet not found", HttpStatus.BAD_REQUEST);
		}
		Wallet wallet = optWwallet.get();

		// Decrypt
		String salt = address + blockchain;
		byte[] nonceByte = wallet.getNonce();
		GCMParameterSpec nonce = new GCMParameterSpec(96, nonceByte);
		SecretKey secretkey = null;
		try {
			secretkey = Encrypt.getKeyFromPassword(password, salt);
		} catch (Exception e) {
			log.error("Error creating secret key: {}", e.getMessage());
			return null;
		}
		String privateKey = null;
		try {
			privateKey = Encrypt.decrypt(wallet.getPrivateKey(), secretkey, nonce);
		} catch (Exception e) {
			log.error("Error decrypting private key: {}", e.getMessage());
			return null;
		}

		// Create new key pair
		ECKeyPair keyPair = ECKeyPair.create(new BigInteger(privateKey));
		return keyPair;
	}
}
