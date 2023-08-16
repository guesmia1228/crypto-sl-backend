package com.nefentus.api.payload.response;

import org.springframework.beans.factory.annotation.Value;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ParentWalletAddresses {
	@Value("${app.name.owner-address}")
	private static String staticOwnerAddress;

	public ParentWalletAddresses() {
		this.ownerAddress = staticOwnerAddress;
	}

	private String ownerAddress;
	private String sellerAddress;
	private String affiliateAddress;
	private String brokerAddress;
	private String leaderAddress;
}
