package com.worker.framework.python;

public class PythonWorkerConf {

    private String codeDir;
    private String PIDDir;
    private String[] command;

    public PythonWorkerConf() {
        super();
    }

    public PythonWorkerConf(String codeDir,
                            String pIDDir,
                            String... command) {
        this.codeDir = codeDir;
        this.PIDDir = pIDDir;
        this.command = command;
    }

    public String getCodeDir() {
        return codeDir;
    }

    public void setCodeDir(String codeDir) {
        this.codeDir = codeDir;
    }

    public String getPIDDir() {
        return PIDDir;
    }

    public void setPIDDir(String pIDDir) {
        PIDDir = pIDDir;
    }

    public String[] getCommand() {
        return command;
    }

    public void setCommand(String[] command) {
        this.command = command;
    }

}
