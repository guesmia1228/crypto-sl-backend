package com.nefentus.api.payload.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MakePaymentRequest {
	private String currencyAddress;
	private String stablecoinAddress;
	private long invoiceId;
	private long productId;
	private BigDecimal amount;
	private int quantity;
	private String password;
}
