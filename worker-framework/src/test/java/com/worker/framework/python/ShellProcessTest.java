package com.worker.framework.python;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.worker.framework.python.PythonResponse.Command;
import com.worker.shared.WorkMessage;
import com.worker.shared.WorkMessageArg;


@RunWith(MockitoJUnitRunner.class)
public class ShellProcessTest {

    @Mock private ObjectMapper objectMapper;
    @InjectMocks ShellProcess shellProcess;

    @Before
    public void assume() {
    	Assume.assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"));
    }

    @Test
    public void test() throws IOException, Exception {
        shellProcess.setObjectMapper(new ObjectMapper());
        boolean destroyed = true;
        File tempDir = Files.createTempDir();
        File pidDir = new File(tempDir, "pids");
        pidDir.mkdir();
        File logDir = new File(tempDir, "logs");
        logDir.mkdir();
        try {
            String tenantId = "mytenant";

            PythonWorkerConnectionUrl url =
                    new PythonWorkerConnectionUrl(tenantId, "postgresql", "host", "dbname", "username", "password");
            List<PythonWorkerConnectionUrl> connectionUrls = Lists.newArrayList(url);

            String pythonPath = LinuxProcessTestUtils.getPythonBinPath();
            PythonWorkerConf pythonWorkerConf =
                    new PythonWorkerConf(".", pidDir.getAbsolutePath(), connectionUrls, pythonPath,
                            "worker_framework/worker/worker.py", logDir.getAbsolutePath() + "/python-%s.log");
            PythonWorkerInitResponse initResponse = shellProcess.launch(pythonWorkerConf);
            Thread.sleep(5000);
            destroyed = false;
            File[] files = pidDir.listFiles();
            Assert.assertNotNull(files);
            Assert.assertEquals(1, files.length);
            String pid = files[0].getName();
            LinuxProcessTestUtils.verifyProcessExists(pid);
            Assert.assertEquals(pid, String.valueOf(initResponse.getPid()));
            Assert.assertTrue(Sets.newHashSet(initResponse.getSupportedTasks()).contains("TestProcessor"));

            List<WorkMessageArg> args =
                    ImmutableList.of(new WorkMessageArg("string", "pong"), new WorkMessageArg("integer", 1111),
                                     new WorkMessageArg("long", 11112132323l), new WorkMessageArg("double", 1.23232d),
                                     new WorkMessageArg("float", 1.1f), new WorkMessageArg("unicode string",
                                             "Ã€Ã�Ã‚ÃƒÃ„Ã…Ã Ã¡Ã¢Ã¤"));
            WorkMessage wm = new WorkMessage("Test", args);
            wm.setLowPriority(true);
            shellProcess.writeMessage(new PythonWorkMessage(tenantId, wm));
            
            PythonResponse response = shellProcess.readResponse();
            Assert.assertEquals(Command.ACK, response.getCommand());
            Assert.assertEquals(1, response.getTriggeredTasks().size());
            WorkMessage triggered = response.getTriggeredTasks().get(0);
            Assert.assertEquals("Test", triggered.getTask());
            Assert.assertEquals(args, triggered.getArgs());
            Assert.assertTrue(triggered.isLowPriority());
            shellProcess.destroy();
            destroyed = true;
            Thread.sleep(200);
            LinuxProcessTestUtils.verifyProcessNotExists(pid);
        } finally {
            if (!destroyed) {
                try {
                    shellProcess.destroy();
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
            FileUtils.deleteQuietly(tempDir);
        }
    }
    
    
    @Test
    public void testGetMessageWithSinkFromPython() throws IOException, Exception {
        shellProcess.setObjectMapper(new ObjectMapper());
        boolean destroyed = true;
        File tempDir = Files.createTempDir();
        File pidDir = new File(tempDir, "pids");
        pidDir.mkdir();
        File logDir = new File(tempDir, "logs");
        logDir.mkdir();
        try {
            String tenantId = "mytenant";

            PythonWorkerConnectionUrl url =
                    new PythonWorkerConnectionUrl(tenantId, "postgresql", "host", "dbname", "username", "password");
            List<PythonWorkerConnectionUrl> connectionUrls = Lists.newArrayList(url);

            String pythonPath = LinuxProcessTestUtils.getPythonBinPath();
            PythonWorkerConf pythonWorkerConf =
                    new PythonWorkerConf(".", pidDir.getAbsolutePath(), connectionUrls, pythonPath,
                            "worker_framework/worker/worker.py", logDir.getAbsolutePath() + "/python-%s.log");
            PythonWorkerInitResponse initResponse = shellProcess.launch(pythonWorkerConf);
            Thread.sleep(5000);
            destroyed = false;
            File[] files = pidDir.listFiles();
            
            Assert.assertNotNull(files);
            Assert.assertEquals(1, files.length);
            String pid = files[0].getName();
            LinuxProcessTestUtils.verifyProcessExists(pid);
            Assert.assertEquals(pid, String.valueOf(initResponse.getPid()));
            Assert.assertTrue(Sets.newHashSet(initResponse.getSupportedTasks()).contains("TestWithSinkProcessor"));
            shellProcess.writeMessage(new PythonWorkMessage(tenantId, new WorkMessage("TestWithSink",new ArrayList<WorkMessageArg>())));
            
            PythonResponse response = shellProcess.readResponse();
            Assert.assertEquals(Command.ACK, response.getCommand());
            Assert.assertEquals(1, response.getTriggeredTasks().size());
            WorkMessage triggered = response.getTriggeredTasks().get(0);
            Assert.assertEquals("TestWithSink", triggered.getTask());
            assertNotNull(triggered.getJoinState());
            assertEquals("join-id", triggered.getJoinState().getJoinId());
            WorkMessage sinkMessage = triggered.getJoinState().getSinkMessage();
            assertEquals("bla", sinkMessage.getTask());
            assertEquals(new WorkMessageArg("name", 1), sinkMessage.getArgs().get(0));
            assertTrue(triggered.isLowPriority());
            
            shellProcess.destroy();
            destroyed = true;
            Thread.sleep(200);
            LinuxProcessTestUtils.verifyProcessNotExists(pid);
        } finally {
            if (!destroyed) {
                try {
                    shellProcess.destroy();
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
            FileUtils.deleteQuietly(tempDir);
        }
    }
}
