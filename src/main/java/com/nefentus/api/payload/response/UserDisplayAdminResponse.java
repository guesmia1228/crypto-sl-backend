package com.nefentus.api.payload.response;

import com.nefentus.api.entities.KycImage;
import com.nefentus.api.entities.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDisplayAdminResponse {
	private Long id;
	private String firstName;
	private String lastName;
	private Set<String> roles;
	private String email;
	private Set<KycImage> kycImages;
	private String tel;
	private String business;
	private String s3Url;
	private boolean activated;
	private BigDecimal Income;
	private Date createdAt;

	public static UserDisplayAdminResponse fromUser(User user) {
		UserDisplayAdminResponse response = new UserDisplayAdminResponse();
		response.setId(user.getId());
		response.setFirstName(user.getFirstName());
		response.setLastName(user.getLastName());
		response.setRoles(user.getRoles().stream()
				.map(role -> role.getName().label.replace("ROLE_", "").replace("_", " ").toLowerCase())
				.collect(Collectors.toSet()));
		// response.setKycImages(user.getKycImage());
		response.setEmail(user.getEmail());
		response.setTel(user.getTel());
		response.setBusiness(user.getBusiness());
		response.setS3Url(user.getS3Url());
		response.setActivated(user.getActive());
		// Set the income field here based on your business logic
		response.setIncome(BigDecimal.valueOf(0));
		response.setCreatedAt(user.getCreatedAt());
		return response;
	}

}
