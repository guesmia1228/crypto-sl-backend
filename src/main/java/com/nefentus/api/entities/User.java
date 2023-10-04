package com.nefentus.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", uniqueConstraints = {
		@UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Size(max = 70)
	@Email
	private String email;

	@Size(max = 120)
	@JsonIgnore
	private String password;

	@Size(max = 120)
	private String firstName;

	@Size(max = 120)
	private String lastName;

	@Size(max = 120)
	private String tel;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "kyc_image", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "kyc_image_id"))
	private Set<KycImage> kyc_image = new HashSet<>();

	@JsonIgnore
	@Column(name = "usr_active")
	private Boolean active;

	@JsonIgnore
	@Column(name = "usr_token")
	private String token;

	@JsonIgnore
	@Column(name = "usr_created_at")
	private Timestamp createdAt;

	@JsonIgnore
	@Column(name = "usr_updated_at")
	private Timestamp updatedAt;

	@JsonIgnore
	@Column(name = "usr_affiliate_link", unique = true)
	private String affiliateLink;

	@JsonIgnore
	@Column(name = "usr_reset_tokem")
	private String resetToken;
	@JsonIgnore
	private String profilePicturepath;
	private String business;
	@JsonIgnore
	private boolean mfa;
	@JsonIgnore
	private String secret;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "seller", cascade = CascadeType.ALL)
	@JsonIgnore
	private Set<Order> orders = new HashSet<>();

	@Lob
	@Column(columnDefinition = "MEDIUMBLOB")
	@JsonIgnore
	private byte[] profilepic;

	@Column(name = "s3_url")
	private String s3Url;

	@Column(name = "is_require_kyc")
	@JsonIgnore
	private boolean isRequireKYC;

	@Column(name = "country")
	private String country;
}
