package com.worker.framework.python;

public class ShellProcessException extends RuntimeException {
    private static final long serialVersionUID = 1L; 
    public ShellProcessException(String message) {
        super(message);
    }
    public ShellProcessException(String message, Throwable cause) {
        super(message, cause);
    }
    public ShellProcessException(Throwable cause) {
        super(cause);
    }    
}
