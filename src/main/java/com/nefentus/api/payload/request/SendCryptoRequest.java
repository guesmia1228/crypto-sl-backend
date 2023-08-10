package com.nefentus.api.payload.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SendCryptoRequest {
	private String tokenAddress;
	private String amount;
	private String toAddress;
	private String password;
}
