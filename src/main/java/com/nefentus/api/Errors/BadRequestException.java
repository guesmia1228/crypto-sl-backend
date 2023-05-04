package com.nefentus.api.Errors;

import org.springframework.http.HttpStatus;

public class BadRequestException extends Exception {
    private final HttpStatus httpStatus;
    public BadRequestException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;

    }
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

}