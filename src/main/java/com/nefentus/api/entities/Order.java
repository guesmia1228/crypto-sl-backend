package com.nefentus.api.entities;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nefentus.api.Services.Web3Service;
import com.nefentus.api.repositories.TransactionRepository;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ord_order")
public class Order {
	@Autowired
	private static TransactionRepository transactionRepository;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ord_id")
	private Long id;

	@Column(name = "ord_created_at")
	private Timestamp createdAt;

	@Column(name = "ord_finished_at")
	private Timestamp finishedAt;

	@Column(name = "ord_updated_at")
	private Timestamp updatedAt;

	@Column(name = "ord_quantity")
	private Integer quantity;

	@Column(name = "ord_total_price")
	private BigDecimal totalPrice;

	@ManyToOne
	@JoinColumn(name = "ord_product_id", referencedColumnName = "prd_id")
	private Product product;

	@ManyToOne
	@JoinColumn(name = "ord_invoice_id", referencedColumnName = "inv_id")
	private Invoice invoice;

	@ManyToOne
	@JoinColumn(name = "ord_seller_id", referencedColumnName = "id")
	@JsonBackReference
	private User seller;

	@Column(name = "ord_currency")
	private String currency;

	@Column(name = "ord_stablecoin")
	private String stablecoin;

	@Column(name = "ord_status")
	private String status;

	private int getStablecoinDigits() {
		Object[] stablecoin = Web3Service.getCurrencyFromAbbr(this.stablecoin);
		return (int) stablecoin[2];
	}

	public BigDecimal getOwnerAmountUSD() {
		Optional<Transaction> optTransaction = Order.transactionRepository.findByOrder(this);
		if (optTransaction.isEmpty()) {
			return new BigDecimal("0");
		}

		int digits = this.getStablecoinDigits();
		return new BigDecimal(optTransaction.get().getOwnerAmount())
				.divide(BigDecimal.valueOf((long) Math.pow(10, digits)));
	}

	public BigDecimal getSellerAmountUSD() {
		Optional<Transaction> optTransaction = Order.transactionRepository.findByOrder(this);
		if (optTransaction.isEmpty()) {
			return new BigDecimal("0");
		}

		int digits = this.getStablecoinDigits();
		return new BigDecimal(optTransaction.get().getSellerAmount())
				.divide(BigDecimal.valueOf((long) Math.pow(10, digits)));
	}

	public BigDecimal getCommissionUSD(Wallet wallet) {
		Optional<Transaction> optTransaction = Order.transactionRepository.findByOrder(this);
		if (optTransaction.isEmpty()) {
			return new BigDecimal("0");
		}

		BigInteger amount = BigInteger.valueOf(0);
		Transaction transaction = optTransaction.get();
		if (transaction.getSellerWallet().getId() == wallet.getId()) {
			amount = transaction.getSellerAmount();
		} else if (transaction.getAffiliateWallet().getId() == wallet.getId()) {
			amount = transaction.getAffiliateAmount();
		} else if (transaction.getBrokerWallet().getId() == wallet.getId()) {
			amount = transaction.getBrokerAmount();
		} else if (transaction.getSeniorBrokerWallet().getId() == wallet.getId()) {
			amount = transaction.getSeniorBrokerAmount();
		} else if (transaction.getLeaderWallet().getId() == wallet.getId()) {
			amount = transaction.getLeaderAmount();
		}

		int digits = this.getStablecoinDigits();
		return new BigDecimal(amount).divide(BigDecimal.valueOf((long) Math.pow(10, digits)));
	}
}