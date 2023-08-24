package com.nefentus.api.Services;

import com.nefentus.api.payload.request.CreateInvoiceRequest;
import com.nefentus.api.repositories.InvoiceRepository;
import com.nefentus.api.repositories.UserRepository;
import com.nefentus.api.security.Id;
import com.nefentus.api.entities.Invoice;
import com.nefentus.api.entities.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {
	private final UserRepository userRepository;
	private final InvoiceRepository invoiceRepository;

	public Optional<Invoice> getInvoice(String invoiceLink) {
		return invoiceRepository.findByLink(invoiceLink);
	}

	public String createInvoice(CreateInvoiceRequest request, String username) {
		User user = userRepository.findUserByEmail(username).orElseThrow();

		Invoice invoice = new Invoice();
		invoice.setCreatedAt(new Timestamp((new Date()).getTime()));
		invoice.setPrice(request.getAmountUSD());
		invoice.setUser(user);

		String invoiceLink = Id.getAlphaNumeric(24);
		while (invoiceRepository.findByLink(invoiceLink).isPresent()) {
			invoiceLink = Id.getAlphaNumeric(24);
		}
		log.info("New invocie id " + invoiceLink);
		invoice.setLink(invoiceLink);

		invoiceRepository.save(invoice);

		return invoiceLink;
	}
}
