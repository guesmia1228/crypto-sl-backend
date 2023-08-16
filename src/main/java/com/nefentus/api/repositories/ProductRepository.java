package com.nefentus.api.repositories;

import com.nefentus.api.entities.Product;
import com.nefentus.api.entities.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
	List<Product> findByUser(User user);

	Optional<Product> findById(Long id);

	Optional<Product> findByLink(String link);
}
