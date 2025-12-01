package com.infy.pinterest.exception;

public class SelfBlockException extends RuntimeException {
    public SelfBlockException(String message) {
        super(message);
    }
}
