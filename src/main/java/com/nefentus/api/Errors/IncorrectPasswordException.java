package com.nefentus.api.Errors;

public class IncorrectPasswordException  extends Exception {
    public IncorrectPasswordException(String message) {
        super(message);
    }
}