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
	public Boolean marketingUpdates;
	public Boolean emailNotifications;
	public Boolean appNotifications;
	public String notificationLanguage;
	public Boolean enableInvoicing;
}
