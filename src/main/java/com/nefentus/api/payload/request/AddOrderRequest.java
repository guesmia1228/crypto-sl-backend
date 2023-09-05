package com.nefentus.api.payload.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddOrderRequest {
	private long productId;
	private long invoiceId;
	private Map<String, Object> transactionInfo;
	private String buyerAddress;
}
