package com.nefentus.api.entities;

import java.math.BigDecimal;
import java.math.BigInteger;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.FetchType;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tra_transaction")
public class Transaction {
	@Id
	@Column(name = "tra_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne
	@JoinColumn(name = "tra_order_id", referencedColumnName = "ord_id")
	private Order order;

	@Column(name = "tra_contract_address")
	private String contractAddress;

	@Column(name = "tra_blockchain")
	private String blockchain;

	@Column(name = "tra_status")
	private String status;

	@Column(name = "tra_gas_price")
	private BigInteger gasPrice;

	@Column(name = "tra_gas_used")
	private BigInteger gasUsed;

	@Column(name = "tra_currency_value")
	private BigInteger currencyValue;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tra_seller_wallet", referencedColumnName = "wlt_id")
	private Wallet sellerWallet;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tra_affiliate_wallet", referencedColumnName = "wlt_id")
	private Wallet affiliateWallet;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tra_broker_wallet", referencedColumnName = "wlt_id")
	private Wallet brokerWallet;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tra_senior_broker_wallet", referencedColumnName = "wlt_id")
	private Wallet seniorBrokerWallet;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tra_leader_wallet", referencedColumnName = "wlt_id")
	private Wallet leaderWallet;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tra_buyer_wallet", referencedColumnName = "wlt_id")
	private Wallet buyerWallet;

	@Column(name = "tra_seller_amount")
	private BigInteger sellerAmount;

	@Column(name = "tra_affiliate_amount")
	private BigInteger affiliateAmount;

	@Column(name = "tra_broker_amount")
	private BigInteger brokerAmount;

	@Column(name = "tra_senior_broker_amount")
	private BigInteger seniorBrokerAmount;

	@Column(name = "tra_leader_amount")
	private BigInteger leaderAmount;

	@Column(name = "tra_owner_amount")
	private BigInteger ownerAmount;

	@Column(name = "tra_swapped_amount")
	private BigInteger swappedAmount;
}