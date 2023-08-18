package com.nefentus.api.repositories;

import com.nefentus.api.entities.Order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
	List<Order> findAllBySellerEmail(String email);
}
