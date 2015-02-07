package com.worker.framework.python;

//when python processor returns error status we throw this exception to return message to queue
public class PythonProcessProcessorException extends PythonProcessException {
    private static final long serialVersionUID = 1L; 
    public PythonProcessProcessorException(String message) {
        super(message);
    }
    public PythonProcessProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
    public PythonProcessProcessorException(Throwable cause) {
        super(cause);
    }
}
