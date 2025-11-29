package com.infy.pinterest.exception;

public class SelfFollowException extends RuntimeException {
    public SelfFollowException(String message) {
        super(message);
    }
}
