package com.infy.pinterest.exception;

public class BusinessProfileAlreadyExistsException extends RuntimeException {
    public BusinessProfileAlreadyExistsException(String message) {
        super(message);
    }
}
