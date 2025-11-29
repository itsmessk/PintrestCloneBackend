package com.infy.pinterest.exception;

public class PinNotFoundException extends RuntimeException {
    public PinNotFoundException(String message) {

        super(message);
    }
}
