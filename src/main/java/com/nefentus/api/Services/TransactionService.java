package com.nefentus.api.Services;

import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.entities.EBlockchain;
import com.nefentus.api.entities.Hierarchy;
import com.nefentus.api.entities.Order;
import com.nefentus.api.entities.Transaction;
import com.nefentus.api.entities.User;
import com.nefentus.api.payload.request.AddOrderRequest;
import com.nefentus.api.payload.response.DashboardNumberResponse;
import com.nefentus.api.repositories.HierarchyRepository;
import com.nefentus.api.repositories.OrderRepository;
import com.nefentus.api.repositories.ProductRepository;
import com.nefentus.api.repositories.TransactionRepository;
import com.nefentus.api.repositories.UserRepository;
import com.nefentus.api.Services.Web3Service;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;

@Service
@AllArgsConstructor
@Slf4j
public class TransactionService {

	private TransactionRepository transactionRepository;
	private ProductRepository productRepository;
	private OrderRepository orderRepository;
	private UserRepository userRepository;
	private HierarchyRepository hierarchyRepository;

	private String toString(Object value) {
		try {
			return value.toString();
		} catch (NullPointerException e) {
			return null;
		}
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
		order.setProduct(request.getProduct());
		order.setSeller(request.getProduct().getUser());

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
		transaction.setSellerAddress(transactionInfo.get("sellerAddress").toString());
		transaction.setBuyerAddress(request.getBuyerAddress());
		transaction.setAffiliateAddress(toString(transactionInfo.get("affiliateAddress")));
		transaction.setBrokerAddress(toString(transactionInfo.get("brokerAddress")));
		transaction.setLeaderAddress(toString(transactionInfo.get("leaderAddress")));
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

	public Map<String, BigDecimal> getTotalPriceByDay() {
		Map<String, BigDecimal> totalPriceByDay = new HashMap<>();
		List<Order> orders = orderRepository.findAll();
		for (Order order : orders) {
			String date = order.getCreatedAt().toLocalDateTime().toLocalDate().toString();
			BigDecimal price = order.getTotalPrice();
			BigDecimal total = totalPriceByDay.getOrDefault(date, new BigDecimal(0));
			totalPriceByDay.put(date, total.add(price));
		}
		log.info("Query successful total income per day!");
		return totalPriceByDay;
	}

	public Map<String, BigDecimal> getTotalPriceByDay(String email) throws UserNotFoundException {
		Optional<User> optUser = userRepository.findUserByEmail(email);

		if (optUser.isEmpty()) {
			log.error("User not found ");
			throw new UserNotFoundException("User not found ", HttpStatus.BAD_REQUEST);
		}

		User user = optUser.get();
		Map<String, BigDecimal> totalPriceByDay = new HashMap<>();

		List<Hierarchy> hierarchies = hierarchyRepository.findAllByParent(user);

		for (var hierarchy : hierarchies) {
			List<Order> orders = orderRepository.findAllBySellerEmail(hierarchy.getChild().getEmail());
			for (Order order : orders) {
				String date = order.getCreatedAt().toLocalDateTime().toLocalDate().toString();
				BigDecimal price = order.getTotalPrice().multiply(hierarchy.getCommissionRate());
				BigDecimal total = totalPriceByDay.getOrDefault(date, new BigDecimal(0));
				totalPriceByDay.put(date, total.add(price));
			}
		}
		log.info("Successful to get total income per day for user with email= {}", user.getEmail());
		return totalPriceByDay;
	}

	public DashboardNumberResponse calculateTotalIncome() {
		BigDecimal totalIncome = getTotalPriceAll();
		BigDecimal lastMonthTotalIncome = getTotalPriceLast30Days();
		BigDecimal last30 = totalIncome.subtract(lastMonthTotalIncome);
		BigDecimal percentageIncrease = last30.compareTo(BigDecimal.valueOf(0)) == 0 ? totalIncome
				: (lastMonthTotalIncome.subtract(last30)).divide(last30).multiply(BigDecimal.valueOf(100));
		DashboardNumberResponse totalIncomeDto = new DashboardNumberResponse();
		totalIncomeDto.setNumber(totalIncome);
		totalIncomeDto.setPercentage(percentageIncrease);
		log.info("Success calculate total income totalIncome:{}", totalIncome);
		return totalIncomeDto;
	}

	public DashboardNumberResponse calculateTotalIncome(String email) throws UserNotFoundException {
		Optional<User> optUser = userRepository.findUserByEmail(email);

		if (optUser.isEmpty()) {
			log.error("User not found");
			throw new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST);
		}
		var user = optUser.get();
		BigDecimal totalIncome = calculateTotalIncomeForUser(user);
		BigDecimal lastMonthTotalIncome = calculateIncomeForUserLast30Days(user);
		BigDecimal last30 = totalIncome.subtract(lastMonthTotalIncome);
		BigDecimal percentageIncrease = last30.compareTo(BigDecimal.valueOf(0)) == 0 ? totalIncome
				: (lastMonthTotalIncome.subtract(last30)).divide(last30).multiply(BigDecimal.valueOf(100));

		DashboardNumberResponse totalIncomeDto = new DashboardNumberResponse();
		totalIncomeDto.setNumber(totalIncome);
		totalIncomeDto.setPercentage(percentageIncrease);
		log.info("Successful calculateTotalIncome for user= {} ", user.getEmail());
		return totalIncomeDto;

	}

	public BigDecimal getTotalPriceByEmail(String email) {
		List<Order> orders = orderRepository.findAllBySellerEmail(email);
		BigDecimal totalPrice = new BigDecimal(0);
		for (Order order : orders) {
			totalPrice = totalPrice.add(order.getTotalPrice());
		}
		return totalPrice;
	}

	public BigDecimal getTotalPriceLast30Days() {
		List<Order> orders = orderRepository.findAll();
		BigDecimal totalPrice = new BigDecimal(0);
		for (Order order : orders) {
			LocalDateTime createdAt = order.getCreatedAt().toLocalDateTime();
			if (createdAt.isAfter(LocalDateTime.now().minusDays(30))) {
				totalPrice = totalPrice.add(order.getTotalPrice());
			}
		}
		return totalPrice;
	}

	public BigDecimal getTotalPriceAll() {
		List<Order> orders = orderRepository.findAll();
		BigDecimal totalPrice = new BigDecimal(0);
		for (Order order : orders) {
			totalPrice = totalPrice.add(order.getTotalPrice());
		}
		return totalPrice;
	}

	public BigDecimal calculateIncomeForUserLast30Days(User user) {
		BigDecimal totalIncome = new BigDecimal(0);

		// Calculate the total income for the user's transactions
		for (Order order : user.getOrders()) {
			LocalDateTime createdAt = order.getCreatedAt().toLocalDateTime();
			if (createdAt.isAfter(LocalDateTime.now().minusDays(30))) {
				totalIncome = totalIncome.add(order.getTotalPrice());
			}
		}

		var hierarchys = hierarchyRepository.findAllByParent(user);

		for (Hierarchy hierarchy : hierarchys) {
			BigDecimal childIncome = calculateTotalIncomeForUser(hierarchy.getChild())
					.multiply(hierarchy.getCommissionRate());
			totalIncome = totalIncome.add(childIncome);
		}

		return totalIncome;
	}

	public BigDecimal calculateTotalIncomeForUser(User user) {
		BigDecimal totalIncome = new BigDecimal(0);

		// Calculate the total income for the user's transactions
		for (Order order : user.getOrders()) {
			totalIncome = totalIncome.add(order.getTotalPrice());
		}

		var hierarchys = hierarchyRepository.findAllByParent(user);

		for (Hierarchy hierarchy : hierarchys) {
			BigDecimal childIncome = calculateTotalIncomeForUser(hierarchy.getChild())
					.multiply(hierarchy.getCommissionRate());
			totalIncome = totalIncome.add(childIncome);
		}

		return totalIncome;
	}

}
