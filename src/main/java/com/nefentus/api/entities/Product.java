package com.nefentus.api.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.nefentus.api.entities.User;

import java.sql.Timestamp;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "prd_product")
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "prd_id")
	private Long id;

	@Column(name = "prd_name")
	private String name;

	@Column(name = "prd_description")
	private String description;

	@Column(name = "prd_price")
	private BigDecimal price;

	// @Column(name = "prd_user_id")
	// private Long user_id;
	@ManyToOne
	@JoinColumn(name = "prd_user_id", referencedColumnName = "id")
	private User user;

	@Column(name = "prd_s3key")
	private String s3Key;

	@Column(name = "prd_stock")
	private Integer stock;

	@Column(name = "prd_created_at")
	private Timestamp createdAt;

	@Column(name = "prd_updated_at")
	private Timestamp updatedAt;
}