package com.infy.pinterest.exception;


public class AccountLockedException extends RuntimeException {
    private final Integer retryAfter;

    public AccountLockedException(String message, Integer retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Integer getRetryAfter() {
        return retryAfter;
    }
}
