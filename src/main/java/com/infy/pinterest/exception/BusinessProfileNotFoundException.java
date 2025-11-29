package com.infy.pinterest.exception;


public class BusinessProfileNotFoundException extends RuntimeException {
    public BusinessProfileNotFoundException(String message) {
        super(message);
    }
}
