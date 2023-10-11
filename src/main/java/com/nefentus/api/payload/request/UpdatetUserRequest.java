package com.nefentus.api.payload.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdatetUserRequest {
	public String firstName;
	public String lastName;
	public String business;
	public String phoneNumber;
	public boolean isMfa;
	public boolean requireOtp;
	public String antiPhishingCode;
}
