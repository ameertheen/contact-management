package com.contactmanagement.exception;

public class DuplicatePhoneException extends RuntimeException {

    public static final String MESSAGE = "This mobile number is already registered.";

    public DuplicatePhoneException() {
        super(MESSAGE);
    }
}
