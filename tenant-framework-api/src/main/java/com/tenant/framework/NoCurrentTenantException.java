package com.tenant.framework;

public class NoCurrentTenantException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NoCurrentTenantException(String message) {
        super(message);
    }

    public NoCurrentTenantException(String message, Throwable cause) {
        super(message, cause);
    }

}
