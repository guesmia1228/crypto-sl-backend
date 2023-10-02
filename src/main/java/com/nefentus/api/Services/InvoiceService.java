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
		invoice.setName(request.getName());
		invoice.setEmail(request.getEmail());
		invoice.setCompany(request.getCompany());
		invoice.setAddress(request.getAddress());
		invoice.setTaxNumber(request.getTaxNumber());
		invoice.setUser(user);

		String invoiceLink = Id.getAlphaNumeric(24);
		while (invoiceRepository.findByLink(invoiceLink).isPresent()) {
			invoiceLink = Id.getAlphaNumeric(24);
		}
		log.info("New invoice id " + invoiceLink);
		invoice.setLink(invoiceLink);

		invoiceRepository.save(invoice);

		return invoiceLink;
	}

	public boolean deleteInvoice(String invoiceLink, String email) {
		Invoice invoice = invoiceRepository.findByLink(invoiceLink).orElseThrow();
		if (invoice.getUser().getEmail().equals(email)) {
			Optional<Invoice> optInvoice = invoiceRepository.findByLink(invoiceLink);
			if (optInvoice.isPresent()) {
				invoiceRepository.delete(optInvoice.get());
				return true;
			}
		}
		return false;
	}
}
