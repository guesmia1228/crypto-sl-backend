package com.nefentus.api.Services;

import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.entities.Hierarchy;
import com.nefentus.api.entities.Transaction;
import com.nefentus.api.entities.User;
import com.nefentus.api.payload.response.DashboardNumberResponse;
import com.nefentus.api.repositories.HierarchyRepository;
import com.nefentus.api.repositories.TransactionRepository;
import com.nefentus.api.repositories.UserRepository;
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

@Service
@AllArgsConstructor
@Slf4j
public class TransactionService {

	private TransactionRepository transactionRepository;
	private UserRepository userRepository;
	private HierarchyRepository hierarchyRepository;

	public Map<String, BigDecimal> getTotalPriceByDay() {
		Map<String, BigDecimal> totalPriceByDay = new HashMap<>();
		List<Transaction> transactions = transactionRepository.findAll();
		for (Transaction transaction : transactions) {
			String date = transaction.getCreatedAt().toLocalDateTime().toLocalDate().toString();
			BigDecimal price = transaction.getTotalPrice();
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
			List<Transaction> transactions = transactionRepository.findAllByUserEmail(hierarchy.getChild().getEmail());
			for (Transaction transaction : transactions) {
				String date = transaction.getCreatedAt().toLocalDateTime().toLocalDate().toString();
				BigDecimal price = transaction.getTotalPrice().multiply(hierarchy.getCommissionRate());
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
		List<Transaction> transactions = transactionRepository.findAllByUserEmail(email);
		BigDecimal totalPrice = new BigDecimal(0);
		for (Transaction transaction : transactions) {
			totalPrice = totalPrice.add(transaction.getTotalPrice());
		}
		return totalPrice;
	}

	public BigDecimal getTotalPriceLast30Days() {
		List<Transaction> transactions = transactionRepository.findAll();
		BigDecimal totalPrice = new BigDecimal(0);
		for (Transaction transaction : transactions) {
			LocalDateTime createdAt = transaction.getCreatedAt().toLocalDateTime();
			if (createdAt.isAfter(LocalDateTime.now().minusDays(30))) {
				totalPrice = totalPrice.add(transaction.getTotalPrice());
			}
		}
		return totalPrice;
	}

	public BigDecimal getTotalPriceAll() {
		List<Transaction> transactions = transactionRepository.findAll();
		BigDecimal totalPrice = new BigDecimal(0);
		for (Transaction transaction : transactions) {
			totalPrice = totalPrice.add(transaction.getTotalPrice());
		}
		return totalPrice;
	}

	public BigDecimal calculateIncomeForUserLast30Days(User user) {
		BigDecimal totalIncome = new BigDecimal(0);

		// Calculate the total income for the user's transactions
		for (Transaction transaction : user.getTransactions()) {
			LocalDateTime createdAt = transaction.getCreatedAt().toLocalDateTime();
			if (createdAt.isAfter(LocalDateTime.now().minusDays(30))) {
				totalIncome = totalIncome.add(transaction.getTotalPrice());
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
		for (Transaction transaction : user.getTransactions()) {
			totalIncome = totalIncome.add(transaction.getTotalPrice());
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
