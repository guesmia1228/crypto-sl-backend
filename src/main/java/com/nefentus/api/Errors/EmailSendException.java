package com.nefentus.api.Errors;

public class EmailSendException extends Exception {
    public EmailSendException(String message) {
        super(message);
    }
}