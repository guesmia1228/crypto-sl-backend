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

@Service
@AllArgsConstructor
@Slf4j
public class TransactionService {

    private TransactionRepository transactionRepository;
    private UserRepository userRepository;
    private HierarchyRepository hierarchyRepository;


    public Map<String, Double> getTotalPriceByDay() {
        Map<String, Double> totalPriceByDay = new HashMap<>();
        List<Transaction> transactions = transactionRepository.findAll();
        for (Transaction transaction : transactions) {
            String date = transaction.getCreatedAt().toLocalDateTime().toLocalDate().toString();
            double price = transaction.getTotalPrice();
            double total = totalPriceByDay.getOrDefault(date, 0.0);
            totalPriceByDay.put(date, total + price);
        }
        log.info("Query successful total income per day!");
        return totalPriceByDay;
    }

    public Map<String, Double> getTotalPriceByDay(String email) throws UserNotFoundException {
        Optional<User> optUser = userRepository.findUserByEmail(email);

        if (optUser.isEmpty()) {
            log.error("User not found ");
            throw new UserNotFoundException("User not found ", HttpStatus.BAD_REQUEST);
        }

        User user = optUser.get();
        Map<String, Double> totalPriceByDay = new HashMap<>();

        List<Hierarchy> hierarchies = hierarchyRepository.findAllByParent(user);

        for (var hierarchy : hierarchies) {
            List<Transaction> transactions = transactionRepository.findAllByUserEmail(hierarchy.getChild().getEmail());
            for (Transaction transaction : transactions) {
                String date = transaction.getCreatedAt().toLocalDateTime().toLocalDate().toString();
                double price = transaction.getTotalPrice() * hierarchy.getCommissionRate();
                double total = totalPriceByDay.getOrDefault(date, 0.0);
                totalPriceByDay.put(date, total + price);
            }
        }
        log.info("Successful to get total income per day for user with email= {}", user.getEmail());
        return totalPriceByDay;
    }

    public DashboardNumberResponse calculateTotalIncome() {
        double totalIncome = getTotalPriceAll();
        double lastMonthTotalIncome = getTotalPriceLast30Days();
        double last30 = totalIncome - lastMonthTotalIncome;
        double percentageIncrease = last30 == 0 ? totalIncome : ((lastMonthTotalIncome - last30) / last30) * 100;
        DashboardNumberResponse totalIncomeDto = new DashboardNumberResponse();
        totalIncomeDto.setNumber(totalIncome);
        totalIncomeDto.setPercentage(Math.round(percentageIncrease));
        log.info("Success calculate total income totalIncome:{}",totalIncome);
        return totalIncomeDto;
    }

    public DashboardNumberResponse calculateTotalIncome(String email) throws UserNotFoundException {
        Optional<User> optUser = userRepository.findUserByEmail(email);

        if (optUser.isEmpty()) {
            log.error("User not found");
            throw new UserNotFoundException("User not found", HttpStatus.BAD_REQUEST);
        }
        var user = optUser.get();
        double totalIncome = calculateTotalIncomeForUser(user);
        double lastMonthTotalIncome = calculateIncomeForUserLast30Days(user);
        double last30 = totalIncome - lastMonthTotalIncome;
        double percentageIncrease = last30 == 0 ? totalIncome : ((lastMonthTotalIncome - last30) / last30) * 100;

        DashboardNumberResponse totalIncomeDto = new DashboardNumberResponse();
        totalIncomeDto.setNumber(totalIncome);
        totalIncomeDto.setPercentage(Math.round(percentageIncrease));
        log.info("Successful calculateTotalIncome for user= {} ", user.getEmail());
        return totalIncomeDto;

    }

    public double getTotalPriceByEmail(String email) {
        List<Transaction> transactions = transactionRepository.findAllByUserEmail(email);
        double totalPrice = 0.0;
        for (Transaction transaction : transactions) {
            totalPrice += transaction.getTotalPrice();
        }
        return totalPrice;
    }

    public double getTotalPriceLast30Days() {
        List<Transaction> transactions = transactionRepository.findAll();
        double totalPrice = 0.0;
        for (Transaction transaction : transactions) {
            LocalDateTime createdAt = transaction.getCreatedAt().toLocalDateTime();
            if (createdAt.isAfter(LocalDateTime.now().minusDays(30))) {
                totalPrice += transaction.getTotalPrice();
            }
        }
        return totalPrice;
    }

    public double getTotalPriceAll() {
        List<Transaction> transactions = transactionRepository.findAll();
        double totalPrice = 0.0;
        for (Transaction transaction : transactions) {
            totalPrice += transaction.getTotalPrice();
        }
        return totalPrice;
    }

    public double calculateIncomeForUserLast30Days(User user) {
        double totalIncome = 0.0;

        // Calculate the total income for the user's transactions
        for (Transaction transaction : user.getTransactions()) {
            LocalDateTime createdAt = transaction.getCreatedAt().toLocalDateTime();
            if (createdAt.isAfter(LocalDateTime.now().minusDays(30))) {
                totalIncome += transaction.getTotalPrice();
            }
        }

        var hierarchys = hierarchyRepository.findAllByParent(user);

        for (Hierarchy hierarchy : hierarchys) {
            double childIncome = calculateTotalIncomeForUser(hierarchy.getChild()) * hierarchy.getCommissionRate();
            totalIncome += childIncome;
        }

        return totalIncome;
    }

    public double calculateTotalIncomeForUser(User user) {
        double totalIncome = 0.0;

        // Calculate the total income for the user's transactions
        for (Transaction transaction : user.getTransactions()) {
            totalIncome += transaction.getTotalPrice();
        }

        var hierarchys = hierarchyRepository.findAllByParent(user);

        for (Hierarchy hierarchy : hierarchys) {
            double childIncome = calculateTotalIncomeForUser(hierarchy.getChild()) * hierarchy.getCommissionRate();
            totalIncome += childIncome;
        }

        return totalIncome;
    }

}
