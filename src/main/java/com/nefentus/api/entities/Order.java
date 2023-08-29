package com.nefentus.api.entities;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ord_order")
public class Order {
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
}