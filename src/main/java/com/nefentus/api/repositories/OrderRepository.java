package com.nefentus.api.repositories;

import com.nefentus.api.entities.Order;
import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {

}
