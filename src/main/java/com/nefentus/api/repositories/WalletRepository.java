package com.nefentus.api.repositories;

import com.nefentus.api.entities.Product;
import com.nefentus.api.entities.Wallet;
import com.nefentus.api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
	List<Wallet> findByOwner(User owner);

	Optional<Wallet> findByAddress(String address);
}
