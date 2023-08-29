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
import com.nefentus.api.payload.request.AddOrderRequest;
import com.nefentus.api.payload.request.MakePaymentRequest;
import com.nefentus.api.payload.response.ParentWalletAddresses;
import com.nefentus.api.Errors.UserAlreadyExistsException;
import com.nefentus.api.repositories.WalletRepository;
import com.nefentus.api.repositories.InvoiceRepository;
import com.nefentus.api.repositories.ProductRepository;
import com.nefentus.api.repositories.UserRepository;
import com.nefentus.api.entities.EBlockchain;
import com.nefentus.api.entities.Hierarchy;
import com.nefentus.api.entities.Invoice;
import com.nefentus.api.entities.Product;
import com.nefentus.api.entities.User;
import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Errors.WalletNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.web3j.protocol.Web3j;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.*;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.generated.*;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Bool;
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
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.lang.InterruptedException;

import com.nefentus.api.security.Encrypt;
import java.math.BigDecimal;

class TransactionResponse {
	TransactionReceipt receipt;
	long timestampSent;
	long timestampMined;
	BigInteger gasPrice;
}

@Service
@RequiredArgsConstructor
@Slf4j
public class Web3Service {
	@Autowired
	private Web3j web3j;

	private final WalletRepository walletRepository;
	private final UserRepository userRepository;
	private final InvoiceRepository invoiceRepository;
	private final ProductRepository productRepository;
	private final UserService userService;
	private final TransactionService transactionService;

	public static final String UNISWAP_ROUTER = "0xE592427A0AEce92De3Edee1F18E0157C05861564";
	public static final String WETH_ADDRESS = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2";
	public static final String USDC_ADDRESS = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48";
	public static final String FACTORY_CONTRACT_ADDRESS = "0x1F98431c8aD98523631AE4a59f267346ea31F984";
	public static final String POOL_FEES = "500";

	public static final String NATIVE_TOKEN = "0x0";
	public static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

	private String nullToZeroAddress(String address) {
		if (address == null)
			return Web3Service.ZERO_ADDRESS;
		return address;
	}

	private List<Type> call(String contractAddress, Function function, String walletAddress) {
		String encodedFunction = FunctionEncoder.encode(function);

		int tries = 0;
		while (tries < 5) {
			try {
				EthCall response = web3j.ethCall(
						Transaction.createEthCallTransaction(walletAddress, contractAddress, encodedFunction),
						DefaultBlockParameterName.LATEST)
						.sendAsync().get();

				List<Type> returnValues = FunctionReturnDecoder.decode(response.getValue(),
						function.getOutputParameters());

				return returnValues;
			} catch (Exception e) {
				log.warn("Error calling smart contract (" + function.getName() + "): " + e.getMessage());
				e.printStackTrace();
			}

			tries += 1;
			try {
				Thread.sleep(100 * tries);
			} catch (InterruptedException e) {
				log.error("Error sleeping: {}", e.getMessage());
			}
		}

		return null;
	}

	private TransactionReceipt transfer(Credentials credentials, String toAddress, String amountEther) {
		int tries = 0;
		while (tries < 5) {
			try {
				TransactionReceipt transactionReceipt = Transfer
						.sendFunds(web3j, credentials, toAddress, new BigDecimal(amountEther), Convert.Unit.ETHER)
						.sendAsync().get();
				return transactionReceipt;
			} catch (Exception e) {
				log.warn("Error calling smart contract (transfer): {}", e.getMessage());
			}

			tries += 1;
			try {
				Thread.sleep(100 * tries);
			} catch (InterruptedException e) {
				log.error("Error sleeping: {}", e.getMessage());
			}
		}

		return null;
	}

	private TransactionResponse transaction(String contractAddress, Function function,
			String walletAddress, Credentials credentials, BigDecimal amountEther, int gasLimit) {
		String encodedFunction = FunctionEncoder.encode(function);
		BigInteger gasPrice = Convert.toWei("20", Convert.Unit.GWEI).toBigInteger();

		TransactionReceipt transactionReceipt = null;
		long timestampSent = 0;
		long timestampMined = 0;
		int tries = 0;
		while (tries < 5) {
			try {
				// Get the nonce
				EthGetTransactionCount ethGetTransactionCount = web3j
						.ethGetTransactionCount(walletAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
				BigInteger nonce = ethGetTransactionCount.getTransactionCount();

				// Create the transaction
				RawTransactionManager rawTransactionManager = new RawTransactionManager(web3j, credentials, (long) 1);
				timestampSent = new Date().getTime();
				EthSendTransaction response = rawTransactionManager.sendTransaction(
						gasPrice, BigInteger.valueOf(gasLimit),
						contractAddress, encodedFunction,
						Convert.toWei(amountEther, Convert.Unit.ETHER).toBigInteger());

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
							timestampMined = new Date().getTime();
							transactionReceipt = getTransactionReceipt.getTransactionReceipt().get();

							TransactionResponse ret = new TransactionResponse();
							ret.receipt = transactionReceipt;
							ret.timestampSent = timestampSent;
							ret.timestampMined = timestampMined;
							ret.gasPrice = gasPrice;
							return ret;
						}

						Thread.sleep(1000);
						tries2++;
					}
				} else {
					log.warn("Error getting transaction hash (" + function.getName() + "): "
							+ response.getError().getMessage());
				}
			} catch (Exception e) {
				log.warn("Error calling smart contract (" + function.getName() + "): " + e.getMessage());
			}

			tries += 1;
			try {
				Thread.sleep(tries * 100);
			} catch (InterruptedException e) {
				log.error("Error sleeping: {}", e.getMessage());
			}
		}

		return null;
	}

	public static Map<String, Object[]> currencyToBlockchain() {
		Map<String, Object[]> map = new HashMap<String, Object[]>();
		// Blockchain, TokenAddress, TokenDecimals
		map.put("ETH", new Object[] { "ETH", NATIVE_TOKEN, 18 });
		map.put("WETH", new Object[] { "ETH", WETH_ADDRESS, 18 });
		map.put("USDT", new Object[] { "ETH", "0xdAC17F958D2ee523a2206206994597C13D831ec7", 6 });
		map.put("USDC", new Object[] { "ETH", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", 6 });
		return map;
	}

	public static Object[] getCurrency(String currencyAddress) {
		if (currencyAddress == null) {
			currencyAddress = Web3Service.NATIVE_TOKEN;
		}

		Map<String, Object[]> currencyToBlockchain = Web3Service.currencyToBlockchain();
		for (String currency : currencyToBlockchain.keySet()) {
			Object[] values = currencyToBlockchain.get(currency);
			if (values[1].equals(currencyAddress)) {
				return values;
			}
		}

		return new Object[] {};
	}

	public static List<Map<String, Object>> contractDeposits() {
		// Read from JSON using Jackson

		List<Map<String, Object>> ret = new ArrayList<>();
		ret.add(Map.of(
				"id", 1,
				"address", "0xC5a70e940925cBF02F093C8Fb20a7202D7afE2C4",
				"abi", "SwapAndDistribute1.json"));
		return ret;
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

		TransactionReceipt transactionReceipt = this.transfer(credentials, toAddress, amount);
		return transactionReceipt != null && transactionReceipt.isStatusOK();
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
		if (keyPair == null) {
			log.error("Error getting keys from wallet");
			return false;
		}
		Credentials credentials = Credentials.create(keyPair);

		// Make input parameters
		List<Type> inputParameters = new ArrayList<>();
		inputParameters.add(new Address(toAddress));
		BigDecimal amountInWei = new BigDecimal(amount).multiply(BigDecimal.valueOf(10).pow(tokenDecimals));
		inputParameters.add(new Uint256(amountInWei.toBigInteger()));

		final Function function = new Function("transfer", inputParameters, Collections.emptyList());
		String encodedFunction = FunctionEncoder.encode(function);

		TransactionResponse response = this.transaction(tokenAddress, function, walletAddress, credentials,
				new BigDecimal(0), 100000);
		if (response != null) {
			return response.receipt.isStatusOK();
		}

		return false;
	}

	public String getPoolAddress(String tokenAddress1, String tokenAddress2, String walletAddress) {
		log.info("Get pool address: {} {}", tokenAddress1, tokenAddress2);
		// Make input parameters
		List<Type> inputParameters = new ArrayList<>();
		inputParameters.add(new Address(tokenAddress1));
		inputParameters.add(new Address(tokenAddress2));
		inputParameters.add(new Uint24(new BigInteger(POOL_FEES)));

		Function function = new Function("getPool", inputParameters, Arrays.asList(new TypeReference<Address>() {
		}));

		List<Type> returnValues = this.call(this.FACTORY_CONTRACT_ADDRESS, function, walletAddress);

		if (returnValues != null && returnValues.size() > 0) {
			return returnValues.get(0).getValue().toString();
		}

		return null;
	}

	public BigDecimal getUniswapPrice(String tokenAddress1, String tokenAddress2, String walletAddress) {
		if (tokenAddress1.equals(this.NATIVE_TOKEN) || tokenAddress2 == null) {
			tokenAddress1 = this.WETH_ADDRESS;
		}
		if (tokenAddress2.equals(this.NATIVE_TOKEN) || tokenAddress2 == null) {
			tokenAddress2 = this.WETH_ADDRESS;
		}

		String poolAddress = this.getPoolAddress(tokenAddress1, tokenAddress2, walletAddress);

		// Make input parameters
		List<TypeReference<?>> outputParameters = Arrays.asList(
				new TypeReference<Uint160>() {
				},
				new TypeReference<Int24>() {
				},
				new TypeReference<Uint16>() {
				},
				new TypeReference<Uint16>() {
				},
				new TypeReference<Uint16>() {
				},
				new TypeReference<Uint8>() {
				},
				new TypeReference<Bool>() {
				});
		Function function = new Function("slot0", Collections.emptyList(), outputParameters);

		List<Type> returnValues = this.call(poolAddress, function, walletAddress);

		if (returnValues != null && returnValues.size() > 0) {
			BigInteger sqrtPriceX96 = new BigInteger(returnValues.get(0).getValue().toString());

			if (sqrtPriceX96 != null)
				return calculatePoolPrice(sqrtPriceX96, tokenAddress1, tokenAddress2);
		}

		return null;
	}

	public BigDecimal calculatePoolPrice(BigInteger sqrtPriceX96, String tokenAddress1, String tokenAddress2) {
		// Get decimals
		int Decimal0 = (int) Web3Service.getCurrency(tokenAddress1)[2];
		int Decimal1 = (int) Web3Service.getCurrency(tokenAddress2)[2];

		BigDecimal buyOneOfToken0 = new BigDecimal(sqrtPriceX96)
				.divide(new BigDecimal(Math.pow(2, 96)));
		buyOneOfToken0 = buyOneOfToken0.pow(2);

		if (Numeric.decodeQuantity(tokenAddress1).compareTo(Numeric.decodeQuantity(tokenAddress2)) == -1) {
			if (Decimal0 > Decimal1)
				buyOneOfToken0 = buyOneOfToken0.multiply(new BigDecimal(10).pow(Decimal0 - Decimal1));
			else
				buyOneOfToken0 = buyOneOfToken0.divide(new BigDecimal(10).pow(Decimal1 - Decimal0), 18,
						RoundingMode.HALF_UP);
			return buyOneOfToken0;
		} else {
			if (Decimal0 > Decimal1)
				buyOneOfToken0 = buyOneOfToken0.divide(new BigDecimal(10).pow(Decimal0 - Decimal1));
			else
				buyOneOfToken0 = buyOneOfToken0.multiply(new BigDecimal(10).pow(Decimal1 - Decimal0));
			BigDecimal buyOneOfToken1 = (new BigDecimal(1)).divide(buyOneOfToken0, 18, RoundingMode.HALF_UP);
			return buyOneOfToken1;
		}
	}

	public boolean makePayment(MakePaymentRequest request, String username)
			throws WalletNotFoundException, UserNotFoundException {

		// Currently, only ETH implemented
		EBlockchain blockchain = EBlockchain.ETHEREUM;
		assert (request.getCurrencyAddress() == null);
		String contractAddress = (String) Web3Service.contractDeposits().get(0).get("address");

		String currencyAddress = request.getCurrencyAddress() != null ? request.getCurrencyAddress()
				: Web3Service.NATIVE_TOKEN;

		// Find user
		Optional<User> optUser = userRepository.findUserByEmail(username);
		if (optUser.isEmpty()) {
			return false;
		}
		User user = optUser.get();

		// Find wallet address
		String walletAddress = null;
		List<Wallet> wallets = walletRepository.findByOwner(user);
		for (Wallet wallet : wallets) {
			if (wallet.getType().equals(blockchain.label)) {
				walletAddress = "0x" + wallet.getAddress();
				break;
			}
		}
		if (walletAddress == null) {
			return false;
		}

		// Find the currency
		Object[] currency = Web3Service.getCurrency(currencyAddress);
		if (currency.length == 0) {
			log.error("Token not found: {}", currencyAddress);
			return false;
		}

		// Find the stablecoin
		Object[] stablecoin = Web3Service.getCurrency(request.getStablecoinAddress());
		if (stablecoin.length == 0) {
			log.error("Token not found: {}", stablecoin);
			return false;
		}

		// Get seller
		User seller;
		if (request.getInvoiceId() != 0) {
			Optional<Invoice> optInvoice = invoiceRepository.findById(request.getInvoiceId());
			seller = optInvoice.get().getUser();
		} else {
			Optional<Product> optProduct = productRepository.findById(request.getProductId());
			seller = optProduct.get().getUser();
		}

		// Get hierarchy
		ParentWalletAddresses hierarchy = userService.getParentWalletAddresses(seller.getId());

		// Get key pair
		String walletAddressWithoutPrefix = walletAddress.substring(2);
		ECKeyPair keyPair = this.keysFromWallet(request.getPassword(), walletAddressWithoutPrefix);
		if (keyPair == null) {
			log.error("Error getting keys from wallet");
			return false;
		}
		Credentials credentials = Credentials.create(keyPair);

		// Get uniswap price
		BigDecimal uniswapPrice = this.getUniswapPrice(currencyAddress, request.getStablecoinAddress(), walletAddress);

		// Ether amount to convert Amount in wei
		BigDecimal amountEther = request.getAmount().divide(uniswapPrice, 18, RoundingMode.UP);
		BigInteger amountWei = Convert.toWei(amountEther, Convert.Unit.ETHER).toBigInteger();

		// Make input parameters
		List<Type> inputParameters = new ArrayList<>();
		inputParameters.add(new Address(hierarchy.getSellerAddress()));
		inputParameters.add(new Address(this.nullToZeroAddress(hierarchy.getAffiliateAddress())));
		inputParameters.add(new Address(this.nullToZeroAddress(hierarchy.getBrokerAddress())));
		inputParameters.add(new Address(this.nullToZeroAddress(hierarchy.getLeaderAddress())));
		inputParameters.add(new Address(request.getStablecoinAddress()));

		BigDecimal minAmountOut = request.getAmount().multiply(new BigDecimal("0.98")).multiply(
				(new BigDecimal(10)).pow((int) stablecoin[2]));
		inputParameters.add(new Uint256(minAmountOut.toBigInteger()));

		inputParameters.add(new Uint24(new BigInteger(POOL_FEES)));

		final Function function = new Function("deposit", inputParameters, Collections.emptyList());

		TransactionResponse response = this.transaction(contractAddress, function, walletAddress, credentials,
				amountEther, 300000);

		if (response != null) {
			if (response.receipt.isStatusOK()) {
				AddOrderRequest results = terminatePayment(response.receipt, response.gasPrice, amountWei,
						response.timestampSent,
						response.timestampMined,
						request.getCurrencyAddress(), request.getStablecoinAddress(), hierarchy, seller, walletAddress,
						request);

				boolean success = transactionService.addTransaction(results);

				return success;
			}
		}

		return false;
	}

	private AddOrderRequest terminatePayment(TransactionReceipt receipt, BigInteger gasPrice, BigInteger value,
			long timestampSent, long timestampMined, String currencyAddress, String stablecoinAddress,
			ParentWalletAddresses hierarchy, User seller, String buyerAddress, MakePaymentRequest request) {
		AddOrderRequest results = new AddOrderRequest();

		Map<String, Object> info = new HashMap<String, Object>();
		info.put("gasPrice", gasPrice);
		info.put("value", value);

		this.parseReceipt(receipt, info);
		info.put("timestampSent", timestampSent);
		info.put("timestampMined", timestampMined);
		info.put("sellerAddress", hierarchy.getSellerAddress());
		info.put("affiliateAddress", hierarchy.getAffiliateAddress());
		info.put("brokerAddress", hierarchy.getBrokerAddress());
		info.put("leaderAddress", hierarchy.getLeaderAddress());
		info.put("currencyAddress", currencyAddress);
		info.put("stablecoinAddress", stablecoinAddress);

		info.put("totalPrice", request.getAmount());
		info.put("quantity", request.getQuantity());

		results.setTransactionInfo(info);
		results.setBuyerAddress(buyerAddress);
		results.setSeller(seller);
		results.setInvoiceId(request.getInvoiceId());
		results.setProductId(request.getProductId());

		return results;
	}

	private void parseReceipt(TransactionReceipt receipt, Map<String, Object> info) {
		// Basic info
		info.put("contractAddress", receipt.getTo());
		info.put("status", Numeric.decodeQuantity(receipt.getStatus()).intValue());

		// Gas & value
		// gasPrice in terminatePayment
		info.put("gasUsed", receipt.getGasUsed());
		// value in terminatePayment

		// Amounts distributed
		Event DistributeEvent = new Event("Distributed",
				Arrays.<TypeReference<?>>asList(
						new TypeReference<Uint256>(false) {
						},
						new TypeReference<Uint256>(false) {
						},
						new TypeReference<Uint256>(false) {
						},
						new TypeReference<Uint256>(false) {
						},
						new TypeReference<Uint256>(false) {
						}));
		String DistributeEventHash = EventEncoder.encode(DistributeEvent);
		for (Log log : receipt.getLogs()) {
			String eventHash = log.getTopics().get(0); // Index 0 is the event definition hash

			if (eventHash.equals(DistributeEventHash)) {
				List<Type> nonIndexedValues = FunctionReturnDecoder.decode(log.getData(),
						DistributeEvent.getNonIndexedParameters());
				BigInteger sellerAmount = new BigInteger(nonIndexedValues.get(0).getValue().toString());
				BigInteger affiliateAmount = new BigInteger(nonIndexedValues.get(1).getValue().toString());
				BigInteger brokerAmount = new BigInteger(nonIndexedValues.get(2).getValue().toString());
				BigInteger leaderAmount = new BigInteger(nonIndexedValues.get(3).getValue().toString());
				BigInteger ownerAmount = new BigInteger(nonIndexedValues.get(4).getValue().toString());

				info.put("sellerAmount", sellerAmount);
				info.put("affiliateAmount", affiliateAmount);
				info.put("brokerAmount", brokerAmount);
				info.put("leaderAmount", leaderAmount);
				info.put("ownerAmount", ownerAmount);
			}
		}

		// Amount swapped
		Event SwapEvent = new Event("Swap",
				Arrays.<TypeReference<?>>asList(
						new TypeReference<Address>(true) {
						},
						new TypeReference<Address>(true) {
						},
						new TypeReference<Int256>(false) {
						},
						new TypeReference<Int256>(false) {
						},
						new TypeReference<Uint160>(false) {
						},
						new TypeReference<Uint128>(false) {
						},
						new TypeReference<Int24>(false) {
						}));
		String SwapEventHash = EventEncoder.encode(SwapEvent);
		for (Log log : receipt.getLogs()) {
			String eventHash = log.getTopics().get(0); // Index 0 is the event definition hash

			if (eventHash.equals(SwapEventHash)) {
				List<Type> nonIndexedValues = FunctionReturnDecoder.decode(log.getData(),
						DistributeEvent.getNonIndexedParameters());
				BigInteger swappedAmount = new BigInteger(nonIndexedValues.get(0).getValue().toString());
				info.put("swappedAmount", swappedAmount);
			}
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
		if (keys2 == null || !keys2.getPrivateKey().toString().equals(keyPair.getPrivateKey().toString())) {
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
