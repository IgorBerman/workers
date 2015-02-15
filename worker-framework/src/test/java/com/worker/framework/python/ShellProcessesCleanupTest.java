package com.worker.framework.python;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.io.Files;
import com.worker.framework.api.WorkerProperties;


@RunWith(MockitoJUnitRunner.class)
public class ShellProcessesCleanupTest {

    @Mock WorkerProperties properties;
    @InjectMocks ShellProcessesCleanup cleanup;
    
    @Before
    public void assume() {
    	Assume.assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"));
    }

    @Test
    public void testCleanupOfProcesses() throws Exception {
        File tempDir = Files.createTempDir();
        File pidDir = new File(tempDir, "pids");
        pidDir.mkdir();
        File logDir = new File(tempDir, "logs");
        logDir.mkdir();
        try {
            String pidDirPath = pidDir.getAbsolutePath();
            pidDir.mkdirs();
            Mockito.when(properties.getPidDir()).thenReturn(pidDirPath);
            Mockito.when(properties.isPythonLogsCleanup()).thenReturn(true);
            String logPath = logDir.getAbsolutePath() + "/python-log-%s.log";
            Mockito.when(properties.getPythonLogPathPrefix()).thenReturn(logPath);
            List<String> exitedProcesses = Collections.synchronizedList(new ArrayList<String>());
            createProcess(exitedProcesses, pidDirPath, logPath);

            Assert.assertNotNull(pidDir.listFiles());
            Assert.assertEquals(1, pidDir.listFiles().length);
            Assert.assertEquals(1, logDir.listFiles().length);
            File pidFile = pidDir.listFiles()[0];
            Assert.assertTrue(pidFile.exists() && pidFile.isFile());
            File logFile = logDir.listFiles()[0];
            Assert.assertTrue(logFile.exists() && logFile.isFile());
            LinuxProcessTestUtils.verifyProcessExists(pidFile.getName());

            cleanup.cleanup();
            Assert.assertTrue(!logFile.exists());
            Assert.assertTrue(!pidFile.exists());
            Assert.assertEquals(0, pidDir.listFiles().length);
            Assert.assertEquals(0, logDir.listFiles().length);
            LinuxProcessTestUtils.verifyProcessNotExists(pidFile.getName());

        } finally {
            FileUtils.deleteQuietly(tempDir);
        }
    }
    
    
    
    @Test
    public void testCleanupOfProcessesNoPythonLogsCleanup() throws Exception {
        File tempDir = Files.createTempDir();
        File pidDir = new File(tempDir, "pids");
        pidDir.mkdir();
        File logDir = new File(tempDir, "logs");
        logDir.mkdir();
        try {
            String pidDirPath = pidDir.getAbsolutePath();
            pidDir.mkdirs();
            Mockito.when(properties.getPidDir()).thenReturn(pidDirPath);
            Mockito.when(properties.isPythonLogsCleanup()).thenReturn(false);
            String logPath = logDir.getAbsolutePath() + "/python-log-%s.log";
            Mockito.when(properties.getPythonLogPathPrefix()).thenReturn(logPath);
            List<String> exitedProcesses = Collections.synchronizedList(new ArrayList<String>());
            createProcess(exitedProcesses, pidDirPath, logPath);

            Assert.assertNotNull(pidDir.listFiles());
            Assert.assertEquals(1, pidDir.listFiles().length);
            Assert.assertEquals(1, logDir.listFiles().length);
            File pidFile = pidDir.listFiles()[0];
            Assert.assertTrue(pidFile.exists() && pidFile.isFile());
            File logFile = logDir.listFiles()[0];
            Assert.assertTrue(logFile.exists() && logFile.isFile());
            LinuxProcessTestUtils.verifyProcessExists(pidFile.getName());

            cleanup.cleanup();
            Assert.assertTrue(logFile.exists());
            Assert.assertTrue(!pidFile.exists());
            Assert.assertEquals(0, pidDir.listFiles().length);
            Assert.assertEquals(1, logDir.listFiles().length);
            LinuxProcessTestUtils.verifyProcessNotExists(pidFile.getName());

        } finally {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    private void createProcess(final List<String> exitedProcesses, String pidDir, String log) throws ExecuteException,
                                                                                             IOException,
                                                                                             InterruptedException {
        CommandLine cmdLine = new CommandLine("python");
        URL filePath = this.getClass().getResource("stuckedprocess.py");
        cmdLine.addArgument(filePath.getPath());
        cmdLine.addArgument(pidDir);
        cmdLine.addArgument(log);

        DefaultExecutor executor = new DefaultExecutor();
        executor.execute(cmdLine, new ExecuteResultHandler() {

            @Override
            public void onProcessComplete(int exitValue) {
                exitedProcesses.add(String.valueOf(exitValue));
            }

            @Override
            public void onProcessFailed(ExecuteException e) {
                exitedProcesses.add(e.getMessage());
            }
        });
        Thread.sleep(1000);
    }

}
