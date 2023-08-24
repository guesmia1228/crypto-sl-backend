package com.nefentus.api.entities;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(name = "inv_invoice")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Invoice {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "inv_id")
	private Long id;

	@Column(name = "inv_link")
	private String link;

	@Column(name = "inv_created_at")
	private Timestamp createdAt;

	@Column(name = "inv_paid_at")
	private Timestamp paidAt;

	@Column(name = "inv_price")
	private BigDecimal price;

	@ManyToOne
	@JoinColumn(name = "inv_user_id", referencedColumnName = "id")
	private User user;
}
