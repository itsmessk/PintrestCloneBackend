package com.infy.pinterest.exception;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {

        super(message);
    }
}
