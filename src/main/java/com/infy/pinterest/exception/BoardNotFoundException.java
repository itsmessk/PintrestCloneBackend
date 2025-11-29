package com.infy.pinterest.exception;

public class BoardNotFoundException extends RuntimeException {
    public BoardNotFoundException(String message) {

        super(message);
    }
}
