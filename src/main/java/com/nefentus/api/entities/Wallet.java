package com.nefentus.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "wlt_wallet")
public class Wallet {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "wlt_id")
	private Long id;

	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "wlt_owner_id", referencedColumnName = "id")
	private User owner;

	@Column(name = "wlt_type")
	private String type;

	@Column(name = "wlt_address")
	private String address;

	@JsonIgnore
	@Column(name = "wlt_private_key")
	private String privateKey;

	@JsonIgnore
	@Column(name = "wlt_nonce")
	private byte[] nonce;

	@Column(name = "wlt_created_at")
	private Timestamp createdAt;
}
