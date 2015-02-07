package com.worker.framework.python;

import java.io.IOException;

import org.junit.Assert;

import com.google.common.base.Strings;


public class LinuxProcessTestUtils {

    public static final void verifyProcessExists(String pid) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("ps -p " + pid);
        int exitStatus = process.waitFor();
        Assert.assertEquals(0, exitStatus);
    }

    public static final void verifyProcessNotExists(String pid) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("ps -p " + pid);
        int exitStatus = process.waitFor();
        Assert.assertEquals(1, exitStatus);
    }

    public static final String getPythonBinPath() {
        //for jenkins, it uses virtual env per build
        String virtualEnvPath = System.getenv("VIRTUAL_ENV");
        if (Strings.isNullOrEmpty(virtualEnvPath)) {
            return System.getenv("HOME") + "/.virtualenvs/backend/bin/python";
        } else {
            return virtualEnvPath + "/bin/python";
        }
    }
}
