package com.nefentus.api.Errors;

public class InactiveUserException extends Exception {
    public InactiveUserException(String message) {
        super(message);
    }
}