package com.nefentus.api.Errors;

import org.springframework.http.HttpStatus;

public class TokenNotFoundException extends Exception {
    private final HttpStatus httpStatus;

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public TokenNotFoundException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus= httpStatus;
    }
}