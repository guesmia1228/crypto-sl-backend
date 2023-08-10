package com.nefentus.api.repositories;

import com.nefentus.api.entities.ERole;
import com.nefentus.api.entities.Product;
import com.nefentus.api.entities.User;
import com.nefentus.api.payload.request.AddProductRequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import microsoft.exchange.webservices.data.property.complex.EmailAddress;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
	List<Product> findByUser(User user);
}
