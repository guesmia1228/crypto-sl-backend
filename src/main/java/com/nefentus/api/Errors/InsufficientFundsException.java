package com.nefentus.api.Errors;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

public class InsufficientFundsException extends Exception {
	private final String message;

	public InsufficientFundsException(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}