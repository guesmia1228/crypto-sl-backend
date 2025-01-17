package com.nefentus.api.entities;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(name = "Hierarchy", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "child_id" })
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Hierarchy {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private User parent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "child_id")
	private User child;

	@Column(name = "commission_rate")
	private BigDecimal commissionRate;

	@Column(name = "created_at")
	private Timestamp createdAt;
}
