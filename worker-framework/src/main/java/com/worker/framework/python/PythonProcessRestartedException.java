package com.worker.framework.python;

//we throw this exception in a case of some system problem in python shell process IO, so that currently processed message will be redelivered
public class PythonProcessRestartedException extends PythonProcessException {
    private static final long serialVersionUID = 1L; 
    public PythonProcessRestartedException(String message) {
        super(message);
    }
    public PythonProcessRestartedException(String message, Throwable cause) {
        super(message, cause);
    }
    public PythonProcessRestartedException(Throwable cause) {
        super(cause);
    }
}
