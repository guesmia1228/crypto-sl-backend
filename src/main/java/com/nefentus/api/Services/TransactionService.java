package com.nefentus.api.Services;

import com.nefentus.api.Errors.InsufficientFundsException;
import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.Errors.WalletNotFoundException;
import com.nefentus.api.entities.EBlockchain;
import com.nefentus.api.entities.Hierarchy;
import com.nefentus.api.entities.Invoice;
import com.nefentus.api.entities.Order;
import com.nefentus.api.entities.Product;
import com.nefentus.api.entities.Transaction;
import com.nefentus.api.entities.User;
import com.nefentus.api.entities.Wallet;
import com.nefentus.api.payload.request.AddOrderRequest;
import com.nefentus.api.payload.request.MakePaymentRequest;
import com.nefentus.api.payload.response.DashboardNumberResponse;
import com.nefentus.api.repositories.HierarchyRepository;
import com.nefentus.api.repositories.InvoiceRepository;
import com.nefentus.api.repositories.OrderRepository;
import com.nefentus.api.repositories.ProductRepository;
import com.nefentus.api.repositories.TransactionRepository;
import com.nefentus.api.repositories.UserRepository;
import com.nefentus.api.repositories.WalletRepository;
import com.nefentus.api.Services.Web3Service;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;

@Service
@AllArgsConstructor
@Slf4j
public class TransactionService {

	private TransactionRepository transactionRepository;
	private ProductRepository productRepository;
	private InvoiceRepository invoiceRepository;
	private OrderRepository orderRepository;
	private UserRepository userRepository;
	private WalletRepository walletRepository;
	private WalletService walletService;
	private Web3Service web3Service;

	private String toString(Object value) {
		try {
			return value.toString();
		} catch (NullPointerException e) {
			return null;
		}
	}

	private Wallet findOrCreateWallet(String addressWithPrefix) {
		String address = addressWithPrefix.substring(2);
		Optional<Wallet> optWallet = walletRepository.findByAddress(address);
		if (optWallet.isEmpty()) {
			return walletService.makeWalletsWithAddress(address);
		} else {
			return optWallet.get();
		}
	}

	public boolean makePayment(MakePaymentRequest request, String username)
			throws WalletNotFoundException, UserNotFoundException, InsufficientFundsException {
		AddOrderRequest results = web3Service.makePayment(request, username);
		if (results != null) {
			boolean success = this.addTransaction(results);
			return success;
		}
		return false;
	}

	public boolean addTransaction(AddOrderRequest request) {
		Map<String, Object> transactionInfo = request.getTransactionInfo();

		String status = ((int) transactionInfo.get("status")) == 1 ? "success" : "error";

		// Order
		Order order = new Order();
		order.setCreatedAt(new Timestamp((long) transactionInfo.get("timestampSent")));
		order.setFinishedAt(new Timestamp((long) transactionInfo.get("timestampMined")));
		order.setUpdatedAt(new Timestamp((long) transactionInfo.get("timestampMined")));
		order.setQuantity(Integer.parseInt(transactionInfo.get("quantity").toString()));
		order.setTotalPrice(new BigDecimal(transactionInfo.get("totalPrice").toString()));

		// Not every order contains a product!!!
		Optional<Product> optProduct = productRepository.findById(request.getProductId());
		order.setProduct(optProduct.isPresent() ? optProduct.get() : null);
		Optional<Invoice> optInvoice = invoiceRepository.findById(request.getInvoiceId());
		order.setInvoice(optInvoice.isPresent() ? optInvoice.get() : null);
		order.setSeller(request.getSeller());

		String currencyAddress = transactionInfo.get("currencyAddress") != null
				? transactionInfo.get("currencyAddress").toString()
				: Web3Service.NATIVE_TOKEN;
		String currency = null;
		for (var entry : Web3Service.currencyToBlockchain().entrySet()) {
			if (entry.getValue()[1].equals(currencyAddress)) {
				currency = entry.getKey();
			}
		}
		order.setCurrency(currency);

		String stablecoinAddress = transactionInfo.get("stablecoinAddress") != null
				? transactionInfo.get("stablecoinAddress").toString()
				: Web3Service.NATIVE_TOKEN;
		String stablecoin = null;
		for (var entry : Web3Service.currencyToBlockchain().entrySet()) {
			if (entry.getValue()[1].equals(stablecoinAddress)) {
				stablecoin = entry.getKey();
			}
		}
		order.setStablecoin(stablecoin);

		order.setStatus(status);

		Order savedOrder = orderRepository.save(order);

		// Transaction
		Transaction transaction = new Transaction();
		transaction.setOrder(savedOrder);
		transaction.setContractAddress(transactionInfo.get("contractAddress").toString());
		transaction.setBlockchain(EBlockchain.ETHEREUM.label);
		transaction.setStatus(status);
		transaction.setGasPrice(new BigInteger(transactionInfo.get("gasPrice").toString()));
		transaction.setGasUsed(new BigInteger(transactionInfo.get("gasUsed").toString()));
		transaction.setCurrencyValue(new BigInteger(transactionInfo.get("value").toString()));
		transaction.setSellerWallet(findOrCreateWallet(transactionInfo.get("sellerAddress").toString()));
		transaction.setBuyerWallet(findOrCreateWallet(request.getBuyerAddress()));
		transaction.setAffiliateWallet(findOrCreateWallet(toString(transactionInfo.get("affiliateAddress"))));
		transaction.setBrokerWallet(findOrCreateWallet(toString(transactionInfo.get("brokerAddress"))));
		transaction.setSeniorBrokerWallet(findOrCreateWallet(toString(transactionInfo.get("seniorBrokerAddress"))));
		transaction.setLeaderWallet(findOrCreateWallet(toString(transactionInfo.get("leaderAddress"))));
		transaction.setSellerAmount(new BigInteger(toString(transactionInfo.get("sellerAmount"))));
		transaction.setAffiliateAmount(new BigInteger(toString(transactionInfo.get("affiliateAmount"))));
		transaction.setBrokerAmount(new BigInteger(toString(transactionInfo.get("brokerAmount").toString())));
		transaction.setLeaderAmount(new BigInteger(toString(transactionInfo.get("leaderAmount").toString())));
		transaction.setOwnerAmount(new BigInteger(toString(transactionInfo.get("ownerAmount"))));
		transaction.setSwappedAmount(new BigInteger(toString(transactionInfo.get("swappedAmount"))));

		transactionRepository.save(transaction);

		log.info("Transaction added successfully");
		return true;
	}

	/**
	 * Get total income (of the platform) per day
	 * This is only called by the admins.
	 * 
	 * @return
	 */
	public Map<String, BigDecimal> getTotalPriceByDay() {
		Map<String, BigDecimal> totalPriceByDay = new HashMap<>();
		List<Order> orders = orderRepository.findAll();
		for (Order order : orders) {
			String date = order.getCreatedAt().toLocalDateTime().toLocalDate().toString();
			BigDecimal price = order.getOwnerAmountUSD();
			BigDecimal total = totalPriceByDay.getOrDefault(date, new BigDecimal(0));
			totalPriceByDay.put(date, total.add(price));
		}
		log.info("Query successful total income per day!");
		return totalPriceByDay;
	}

	/**
	 * Get total income of a user per day. Orders as a vendor as well as leader, ...
	 * are considered
	 * 
	 * @param email
	 * @return
	 * @throws UserNotFoundException
	 */
	public Map<String, BigDecimal> getTotalPriceByDay(String email) throws UserNotFoundException {
		Optional<User> optUser = userRepository.findUserByEmail(email);
		if (optUser.isEmpty()) {
			log.error("User not found ");
			throw new UserNotFoundException("User not found ", HttpStatus.BAD_REQUEST);
		}

		User user = optUser.get();
		Map<String, BigDecimal> totalPriceByDay = this.getTotalPriceByDayAsVendor(email);

		// Get wallet addresses
		List<Wallet> wallets = user.getWallets();
		for (Wallet wallet : wallets) {
			for (Order order : orderRepository.findAll()) {
				String date = order.getCreatedAt().toLocalDateTime().toLocalDate().toString();
				BigDecimal price = order.getCommissionUSD(wallet);
				BigDecimal total = totalPriceByDay.getOrDefault(date, new BigDecimal(0));
				totalPriceByDay.put(date, total.add(price));
			}
		}
		log.info("Successful to get total income per day for user with email= {}", user.getEmail());
		return totalPriceByDay;
	}

	/**
	 * Get total income (of a vendor) per day. This is only called by a vendor.
	 * 
	 * @param email
	 * @return
	 * @throws UserNotFoundException
	 */
	public Map<String, BigDecimal> getTotalPriceByDayAsVendor(String email) throws UserNotFoundException {
		Optional<User> optUser = userRepository.findUserByEmail(email);
		if (optUser.isEmpty()) {
			log.error("User not found ");
			throw new UserNotFoundException("User not found ", HttpStatus.BAD_REQUEST);
		}

		Map<String, BigDecimal> totalPriceByDay = new HashMap<>();

		// Orders of products of the user
		List<Order> userOrders = orderRepository.findAllBySellerEmail(email);
		for (Order order : userOrders) {
			String date = order.getCreatedAt().toLocalDateTime().toLocalDate().toString();
			BigDecimal price = order.getSellerAmountUSD();
			BigDecimal total = totalPriceByDay.getOrDefault(date, new BigDecimal(0));
			totalPriceByDay.put(date, total.add(price));
		}

		return totalPriceByDay;
	}

	/**
	 * Get the income of the owner of the platform
	 * 
	 * @return
	 */
	public DashboardNumberResponse calculateTotalIncome() {
		BigDecimal totalIncome = getOwnerIncomeAll();
		BigDecimal lastMonthTotalIncome = getOwnerIncomeLast30Days();
		BigDecimal last30 = totalIncome.subtract(lastMonthTotalIncome);
		BigDecimal percentageIncrease = last30.compareTo(BigDecimal.valueOf(0)) == 0 ? totalIncome
				: (lastMonthTotalIncome.subtract(last30)).divide(last30).multiply(BigDecimal.valueOf(100));
		DashboardNumberResponse totalIncomeDto = new DashboardNumberResponse();
		totalIncomeDto.setNumber(totalIncome);
		totalIncomeDto.setPercentage(percentageIncrease);
		log.info("Success calculate total income totalIncome:{}", totalIncome);
		return totalIncomeDto;
	}

	/**
	 * Get the income of a user (as vendor or something else)
	 * 
	 * @param userEmail
	 * @return
	 * @throws UserNotFoundException
	 */
	public DashboardNumberResponse calculateTotalIncome(String userEmail) throws UserNotFoundException {
		Optional<User> optUser = userRepository.findUserByEmail(userEmail);
		if (optUser.isEmpty()) {
			log.error("User not found ");
			throw new UserNotFoundException("User not found ", HttpStatus.BAD_REQUEST);
		}
		User user = optUser.get();

		BigDecimal totalIncome = getIncomeForUser(user);
		BigDecimal lastMonthTotalIncome = calculateIncomeForUserLast30Days(user);
		BigDecimal beforeIncome = totalIncome.subtract(lastMonthTotalIncome);
		BigDecimal percentageIncrease = beforeIncome.compareTo(BigDecimal.valueOf(0)) == 0 ? null
				: (lastMonthTotalIncome.subtract(beforeIncome)).divide(beforeIncome, 4, RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100));

		DashboardNumberResponse totalIncomeDto = new DashboardNumberResponse();
		totalIncomeDto.setNumber(totalIncome);
		totalIncomeDto.setPercentage(percentageIncrease);
		log.info("Successful calculateTotalIncome for user= {} ", user.getEmail());
		return totalIncomeDto;
	}

	public DashboardNumberResponse calculateIncomeLast30Days(String userEmail) throws UserNotFoundException {
		Optional<User> optUser = userRepository.findUserByEmail(userEmail);
		if (optUser.isEmpty()) {
			log.error("User not found ");
			throw new UserNotFoundException("User not found ", HttpStatus.BAD_REQUEST);
		}
		User user = optUser.get();

		BigDecimal last30Days = calculateIncomeForUserLast30Days(user);
		BigDecimal beforeIncome = calculateIncomeForUserBetween(user, 60, 30);
		BigDecimal percentageIncrease = beforeIncome.compareTo(BigDecimal.valueOf(0)) == 0 ? null
				: (last30Days.subtract(beforeIncome)).divide(beforeIncome, 4, RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100));

		DashboardNumberResponse incomeLast30Days = new DashboardNumberResponse();
		incomeLast30Days.setNumber(last30Days);
		incomeLast30Days.setPercentage(percentageIncrease);
		log.info("Successful calculateIncomeLast30Days for user= {} ", user.getEmail());
		return incomeLast30Days;
	}

	public DashboardNumberResponse calculateIncomeLast24Hours(String userEmail) throws UserNotFoundException {
		Optional<User> optUser = userRepository.findUserByEmail(userEmail);
		if (optUser.isEmpty()) {
			log.error("User not found ");
			throw new UserNotFoundException("User not found ", HttpStatus.BAD_REQUEST);
		}
		User user = optUser.get();

		BigDecimal last30Days = calculateIncomeForUserBetween(user, 1, 0);
		BigDecimal beforeIncome = calculateIncomeForUserBetween(user, 2, 1);
		BigDecimal percentageIncrease = beforeIncome.compareTo(BigDecimal.valueOf(0)) == 0 ? null
				: (last30Days.subtract(beforeIncome)).divide(beforeIncome, 4, RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100));

		DashboardNumberResponse incomeLast24Hours = new DashboardNumberResponse();
		incomeLast24Hours.setNumber(last30Days);
		incomeLast24Hours.setPercentage(percentageIncrease);
		log.info("Successful calculateIncomeLast24Hours for user= {} ", user.getEmail());
		return incomeLast24Hours;
	}

	public DashboardNumberResponse getNumberOfOrders(String userEmail) {
		int numberOfOrders = this.getNumberOfOrdersForUser(userEmail);
		int numberOfOrdersThisMonth = this.getNumberOfOrdersForUserBetween(userEmail, 30, 0);
		int numberOfOrdersBefore = numberOfOrders - numberOfOrdersThisMonth;
		BigDecimal percentageIncrease = numberOfOrdersBefore == 0 ? null
				: BigDecimal.valueOf(((numberOfOrders - numberOfOrdersBefore) / numberOfOrdersBefore) * 100.0);

		DashboardNumberResponse ret = new DashboardNumberResponse();
		ret.setNumber(BigDecimal.valueOf(numberOfOrders));
		ret.setPercentage(percentageIncrease);
		log.info("Successful getNumberOfOrders for user= {} ", userEmail);
		return ret;
	}

	public int getNumberOfOrdersForUser(String email) {
		List<Order> orders = orderRepository.findAllBySellerEmail(email);
		return orders.size();
	}

	public int getNumberOfOrdersForUserBetween(String email, int daysStart, int daysEnd) {
		List<Order> orders = orderRepository.findAllBySellerEmail(email);
		int numberOfOrders = 0;

		LocalDateTime start = LocalDateTime.now().minusDays(daysStart);
		LocalDateTime end = LocalDateTime.now().minusDays(daysEnd);

		for (Order order : orders) {
			LocalDateTime createdAt = order.getCreatedAt().toLocalDateTime();
			if (createdAt.isAfter(start) && createdAt.isBefore(end)) {
				numberOfOrders++;
			}
		}
		return numberOfOrders;
	}

	/**
	 * Get the total income of the platform in the last 30 days
	 */
	public BigDecimal getOwnerIncomeLast30Days() {
		List<Order> orders = orderRepository.findAll();
		BigDecimal totalPrice = new BigDecimal(0);
		for (Order order : orders) {
			LocalDateTime createdAt = order.getCreatedAt().toLocalDateTime();
			if (createdAt.isAfter(LocalDateTime.now().minusDays(30))) {
				totalPrice = totalPrice.add(order.getOwnerAmountUSD());
			}
		}
		return totalPrice;
	}

	/**
	 * Get the income of the owner of the platform
	 */
	public BigDecimal getOwnerIncomeAll() {
		List<Order> orders = orderRepository.findAll();
		BigDecimal totalPrice = new BigDecimal(0);
		for (Order order : orders) {
			totalPrice = totalPrice.add(order.getOwnerAmountUSD());
		}
		return totalPrice;
	}

	public BigDecimal calculateIncomeForUserLast30Days(User user) {
		return calculateIncomeForUserBetween(user, 30, 0);
	}

	/**
	 * Calculate the income in the past between daysStart before now and daysEnd
	 * before now.
	 * 
	 * @param user      The user to calculate the income for
	 * @param daysStart The number of days before now to start calculating the
	 *                  income
	 * @param daysEnd   The number of days before now to end calculating the income
	 * @return
	 */
	public BigDecimal calculateIncomeForUserBetween(User user, int daysStart, int daysEnd) {
		BigDecimal totalIncome = new BigDecimal(0);

		LocalDateTime start = LocalDateTime.now().minusDays(daysStart);
		LocalDateTime end = LocalDateTime.now().minusDays(daysEnd);

		// Calculate the total income for the user's transactions
		for (Order order : user.getOrders()) {
			LocalDateTime createdAt = order.getCreatedAt().toLocalDateTime();
			if (createdAt.isAfter(start) && createdAt.isBefore(end)) {
				totalIncome = totalIncome.add(order.getTotalPrice());
			}
		}

		List<Wallet> wallets = user.getWallets();
		for (Wallet wallet : wallets) {
			for (Order order : orderRepository.findAll()) {
				LocalDateTime createdAt = order.getCreatedAt().toLocalDateTime();
				if (createdAt.isAfter(start) && createdAt.isBefore(end)) {
					BigDecimal price = order.getCommissionUSD(wallet);
					totalIncome = totalIncome.add(price);
				}
			}
		}

		return totalIncome;
	}

	public BigDecimal getIncomeForUser(User user) {
		BigDecimal totalIncome = new BigDecimal(0);

		// Calculate the total income for the user's transactions
		for (Order order : user.getOrders()) {
			totalIncome = totalIncome.add(order.getSellerAmountUSD());
		}

		// Get wallet addresses
		List<Wallet> wallets = user.getWallets();
		for (Wallet wallet : wallets) {
			for (Order order : orderRepository.findAll()) {
				BigDecimal price = order.getCommissionUSD(wallet);
				totalIncome = totalIncome.add(price);
			}
		}

		return totalIncome;
	}

}
