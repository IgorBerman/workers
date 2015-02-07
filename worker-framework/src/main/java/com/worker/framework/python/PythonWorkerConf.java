package com.worker.framework.python;

import java.util.List;


public class PythonWorkerConf {

    private String codeDir;
    private String PIDDir;
    private String[] command;
    private List<PythonWorkerConnectionUrl> connectionUrls;

    public PythonWorkerConf() {
        super();
    }

    public PythonWorkerConf(String codeDir,
                            String pIDDir,
                            List<PythonWorkerConnectionUrl> connectionUrls,
                            String... command) {
        this.codeDir = codeDir;
        this.PIDDir = pIDDir;
        this.connectionUrls = connectionUrls;
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

    public List<PythonWorkerConnectionUrl> getConnectionUrls() {
        return connectionUrls;
    }

}
