package com.nefentus.api.repositories;

import com.nefentus.api.entities.Invoice;
import com.nefentus.api.entities.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
	List<Invoice> findByUser(User user);

	Optional<Invoice> findById(long id);

	Optional<Invoice> findByLink(String link);
}
