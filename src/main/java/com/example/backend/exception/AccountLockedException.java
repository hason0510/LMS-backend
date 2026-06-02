package com.example.backend.exception;

import org.springframework.security.core.AuthenticationException;

public class AccountLockedException extends AuthenticationException {
    public static final String ERROR_KEY = "ACCOUNT_LOCKED";

    public AccountLockedException() {
        super(ERROR_KEY);
    }

    public AccountLockedException(String message) {
        super(message);
    }
}
