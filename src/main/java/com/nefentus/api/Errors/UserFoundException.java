package com.nefentus.api.Errors;

import org.springframework.http.HttpStatus;

public class UserFoundException extends Exception {
	private final HttpStatus httpStatus;

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public UserFoundException(String message, HttpStatus httpStatus) {
		super(message);
		this.httpStatus = httpStatus;
	}

}
