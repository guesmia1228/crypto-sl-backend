package com.nefentus.api.Errors;

import org.springframework.http.HttpStatus;

public class WalletNotFoundException extends Exception {
	private final HttpStatus httpStatus;

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public WalletNotFoundException(String message, HttpStatus httpStatus) {
		super(message);
		this.httpStatus = httpStatus;
	}
}
