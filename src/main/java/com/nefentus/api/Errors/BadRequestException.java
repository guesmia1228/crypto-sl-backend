package com.nefentus.api.Errors;

public class BadRequestException extends Exception {
    public BadRequestException(String message) {
        super(message);
    }
}