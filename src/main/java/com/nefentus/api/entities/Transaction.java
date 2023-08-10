package com.nefentus.api.entities;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@Entity
@Table(name = "tra_transaction")
@AllArgsConstructor
public class Transaction {
	@Id
	@Column(name = "tra_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "tra_payment_method")
	private String paymentMethod;
	@Column(name = "tra_created_at")
	private Timestamp createdAt;

	@Column(name = "tra_total_price")
	private BigDecimal totalPrice;

	@ManyToOne
	@JoinColumn(name = "tra_user_id", referencedColumnName = "id")
	private User user;

}