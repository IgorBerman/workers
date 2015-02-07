package com.worker.framework.python;

public class PythonProcessException extends RuntimeException {
    private static final long serialVersionUID = 1L; 
    public PythonProcessException(String message) {
        super(message);
    }
    public PythonProcessException(String message, Throwable cause) {
        super(message, cause);
    }
    public PythonProcessException(Throwable cause) {
        super(cause);
    }
}
