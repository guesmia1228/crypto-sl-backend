package com.nefentus.api.Errors;

import org.springframework.http.HttpStatus;

public class EmailSendException extends Exception {
    private final HttpStatus httpStatus;

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }


    public EmailSendException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }
}