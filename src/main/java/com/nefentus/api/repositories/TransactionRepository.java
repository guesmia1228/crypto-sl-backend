package com.nefentus.api.repositories;

import com.nefentus.api.entities.Transaction;
import com.nefentus.api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByCreatedAtAfter(Timestamp timestamp);
    List<Transaction> findAllByUserEmail(String email);
    List<Transaction> findAllByUserIn(List<User> users);
}
