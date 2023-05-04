package com.nefentus.api.Errors;

import org.springframework.http.HttpStatus;

public class InactiveUserException extends Exception {
    private final HttpStatus httpStatus;

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public InactiveUserException(String message,HttpStatus httpStatus ) {
        super(message);
        this.httpStatus = httpStatus;
    }


}