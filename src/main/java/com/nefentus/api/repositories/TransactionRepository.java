package com.nefentus.api.repositories;

import com.nefentus.api.entities.Transaction;
import com.nefentus.api.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
	Optional<Transaction> findByOrder(Order order);
}
