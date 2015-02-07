package com.worker.framework.python;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.tenant.framework.CurrentTenant;
import com.worker.framework.python.PythonResponse.Command;
import com.worker.shared.WorkMessage;


public class PerThreadPythonWorkerTest {

    @Mock private Provider<ShellProcess> processProvider;
    @Mock private ShellProcess process;
    @Mock private CurrentTenant currentTenant;
    @Mock private PythonWorkerConf pythonWorkerConf;
    @Mock private ShellProcessesCleanup processCleanup;

    @InjectMocks PerThreadPythonWorker pythonWorker;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = PythonProcessException.class)
    public void testUnsuccessfullStart() throws IOException {
        Mockito.when(processProvider.get()).thenReturn(process);
        PythonWorkerInitResponse initResponse = new PythonWorkerInitResponse(1);
        List<String> supportedTasks = ImmutableList.of("aProcessor", "bProcessor", "cProcessor");
        initResponse.setSupportedTasks(supportedTasks);
        Mockito.when(process.launch(pythonWorkerConf)).thenThrow(new IOException());

        pythonWorker.start();
    }

    @Test(expected = PythonProcessException.class)
    public void testOnResponseInteruption() throws IOException {
        try {
            Mockito.when(processProvider.get()).thenReturn(process);
            PythonWorkerInitResponse initResponse = new PythonWorkerInitResponse(1);
            List<String> supportedTasks = ImmutableList.of("aProcessor", "bProcessor", "cProcessor");
            initResponse.setSupportedTasks(supportedTasks);
            Mockito.when(process.launch(pythonWorkerConf)).thenReturn(initResponse);
            Mockito.when(process.readResponse()).thenReturn(null);// so that main(consumer) will wait for response
                                                                  // infinitely, but we will interrupt it

            pythonWorker.start();

            final Thread mainThread = Thread.currentThread();
            new Thread(new Runnable() {// we open another thread that will interupt main thread

                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mainThread.interrupt();
                        }
                    }).start();
            WorkMessage input = new WorkMessage();
            pythonWorker.executePython(input);
        } finally {
            pythonWorker.stop();
        }
    }

    @Test(expected = PythonProcessException.class)
    public void testOnRequestPutInteruption() throws IOException {
        try {
            Mockito.when(processProvider.get()).thenReturn(process);
            PythonWorkerInitResponse initResponse = new PythonWorkerInitResponse(1);
            List<String> supportedTasks = ImmutableList.of("aProcessor", "bProcessor", "cProcessor");
            initResponse.setSupportedTasks(supportedTasks);
            Mockito.when(process.launch(pythonWorkerConf)).thenReturn(initResponse);
            Mockito.when(process.readResponse()).thenReturn(null);// so that main(consumer) will wait for response
                                                                  // infinitely, but we will interrupt it

            pythonWorker.start();// starts reader & writer

            // stop writer manually so that put will stuck
            pythonWorker.getWriterThread().requestStop();
            try {
                pythonWorker.getWriterThread().join();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            final Thread mainThread = Thread.currentThread();
            new Thread(new Runnable() {// we open another thread that will interupt main thread

                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mainThread.interrupt();
                        }
                    }).start();
            WorkMessage input = new WorkMessage();
            pythonWorker.executePython(input);
        } finally {
            pythonWorker.stop();
        }
    }

    @Test
    public void testAckMessage() throws IOException {
        Mockito.when(processProvider.get()).thenReturn(process);
        PythonWorkerInitResponse initResponse = new PythonWorkerInitResponse(1);
        List<String> supportedTasks = ImmutableList.of("aProcessor", "bProcessor", "cProcessor");
        initResponse.setSupportedTasks(supportedTasks);
        Mockito.when(process.launch(pythonWorkerConf)).thenReturn(initResponse);
        PythonResponse response = new PythonResponse();
        response.setCommand(Command.ACK);
        Mockito.when(process.readResponse()).thenReturn(response);

        pythonWorker.start();

        WorkMessage input = new WorkMessage();
        PythonResponse pythonResponse = pythonWorker.executePython(input);
        Assert.assertEquals(pythonResponse, response);
        Assert.assertTrue(pythonWorker.supportsTask("a"));
        Assert.assertTrue(pythonWorker.supportsTask("b"));
        Assert.assertTrue(pythonWorker.supportsTask("c"));
        Assert.assertFalse(pythonWorker.supportsTask("d"));
        Assert.assertEquals(1, pythonWorker.getSubpid().intValue());
        Assert.assertNotNull(pythonWorker.getReaderThread());
        Assert.assertNotNull(pythonWorker.getWriterThread());
    }

    @Test(expected = PythonProcessProcessorException.class)
    public void testErrorMessage() throws IOException {
        try {
            Mockito.when(processProvider.get()).thenReturn(process);
            PythonWorkerInitResponse initResponse = new PythonWorkerInitResponse(1);
            List<String> supportedTasks = ImmutableList.of("aProcessor", "bProcessor", "cProcessor");
            initResponse.setSupportedTasks(supportedTasks);
            Mockito.when(process.launch(pythonWorkerConf)).thenReturn(initResponse);
            PythonResponse response = new PythonResponse();
            response.setCommand(Command.ERROR);
            response.setMsg("some problem in python");
            Mockito.when(process.readResponse()).thenReturn(response);

            pythonWorker.start();

            WorkMessage input = new WorkMessage();
            pythonWorker.executePython(input);
        } finally {
            pythonWorker.stop();
        }
    }
    
    
    @Test(expected = PythonProcessProcessorException.class)
    public void testLongErrorMessage() throws IOException {
        try {
            Mockito.when(processProvider.get()).thenReturn(process);
            PythonWorkerInitResponse initResponse = new PythonWorkerInitResponse(1);
            List<String> supportedTasks = ImmutableList.of("aProcessor", "bProcessor", "cProcessor");
            initResponse.setSupportedTasks(supportedTasks);
            Mockito.when(process.launch(pythonWorkerConf)).thenReturn(initResponse);
            PythonResponse response = new PythonResponse();
            response.setCommand(Command.ERROR);
            response.setMsg(Strings.repeat("e", PerThreadPythonWorker.RABBITMQ_FRAME_MAX));
            Mockito.when(process.readResponse()).thenReturn(response);

            pythonWorker.start();

            WorkMessage input = new WorkMessage();
            try {
                pythonWorker.executePython(input);
            } catch (PythonProcessProcessorException e) {
                assertTrue(e.getMessage().length() <= (int)(9.0*PerThreadPythonWorker.RABBITMQ_FRAME_MAX/10.0));
                throw e;
            }
        } finally {
            pythonWorker.stop();
        }
    }
    
    @Test(expected = PythonProcessProcessorException.class)
    public void testNullErrorMessage() throws IOException {
        try {
            Mockito.when(processProvider.get()).thenReturn(process);
            PythonWorkerInitResponse initResponse = new PythonWorkerInitResponse(1);
            List<String> supportedTasks = ImmutableList.of("aProcessor", "bProcessor", "cProcessor");
            initResponse.setSupportedTasks(supportedTasks);
            Mockito.when(process.launch(pythonWorkerConf)).thenReturn(initResponse);
            PythonResponse response = new PythonResponse();
            response.setCommand(Command.ERROR);
            response.setMsg(null);
            Mockito.when(process.readResponse()).thenReturn(response);

            pythonWorker.start();

            WorkMessage input = new WorkMessage();
            pythonWorker.executePython(input);
        } finally {
            pythonWorker.stop();
        }
    }

    @Test
    public void testStopAndRestartDueToReaderException() throws IOException {
        try {
            Mockito.when(processProvider.get()).thenReturn(process);
            PythonWorkerInitResponse initResponse = new PythonWorkerInitResponse(1);
            List<String> supportedTasks = ImmutableList.of("aProcessor", "bProcessor", "cProcessor");
            initResponse.setSupportedTasks(supportedTasks);
            PythonWorkerInitResponse initResponseAfterRestart = new PythonWorkerInitResponse(2);
            List<String> supportedTasksAfterRestart = ImmutableList.of("aProcessor", "dProcessor", "cProcessor");
            initResponseAfterRestart.setSupportedTasks(supportedTasksAfterRestart);
            Mockito.when(process.launch(pythonWorkerConf))
                   .thenReturn(initResponse)
                   .thenReturn(initResponseAfterRestart);
            PythonResponse response = new PythonResponse();
            response.setCommand(Command.ACK);
            Mockito.when(process.readResponse())
                   .thenThrow(new IOException("reader throws io exception"))
                   .thenReturn(response);
            pythonWorker.start();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            WorkMessage input = new WorkMessage();
            Assert.assertTrue(pythonWorker.supportsTask("a"));
            Assert.assertTrue(pythonWorker.supportsTask("b"));
            Assert.assertTrue(pythonWorker.supportsTask("c"));
            Assert.assertFalse(pythonWorker.supportsTask("d"));
            Assert.assertEquals(1, pythonWorker.getSubpid().intValue());
            Thread readerThread = pythonWorker.getReaderThread();
            Assert.assertNotNull(readerThread);
            Thread writerThread = pythonWorker.getWriterThread();
            Assert.assertNotNull(writerThread);
            try {
                pythonWorker.executePython(input);
            } catch (PythonProcessException e) {
                Assert.assertEquals(e.getClass(), PythonProcessRestartedException.class);
            }
            Assert.assertFalse(readerThread.isAlive());
            Assert.assertFalse(writerThread.isAlive());

            Mockito.verify(processCleanup, Mockito.times(1)).killProcess(1);

            PythonResponse pythonResponse = pythonWorker.executePython(input);
            Assert.assertEquals(pythonResponse, response);
            Assert.assertTrue(pythonWorker.supportsTask("a"));
            Assert.assertFalse(pythonWorker.supportsTask("b"));
            Assert.assertTrue(pythonWorker.supportsTask("c"));
            Assert.assertTrue(pythonWorker.supportsTask("d"));
            Assert.assertEquals(2, pythonWorker.getSubpid().intValue());
            Assert.assertFalse(readerThread == pythonWorker.getReaderThread());
            Assert.assertFalse(writerThread == pythonWorker.getWriterThread());
        } finally {
            pythonWorker.stop();
        }
    }

    @Test
    public void testStopAndRestartDueToWriterException() throws IOException {
        try {
            Mockito.when(processProvider.get()).thenReturn(process);
            PythonWorkerInitResponse initResponse = new PythonWorkerInitResponse(1);
            List<String> supportedTasks = ImmutableList.of("aProcessor", "bProcessor", "cProcessor");
            initResponse.setSupportedTasks(supportedTasks);
            PythonWorkerInitResponse initResponseAfterRestart = new PythonWorkerInitResponse(2);
            List<String> supportedTasksAfterRestart = ImmutableList.of("aProcessor", "dProcessor", "cProcessor");
            initResponseAfterRestart.setSupportedTasks(supportedTasksAfterRestart);
            Mockito.when(process.launch(pythonWorkerConf))
                   .thenReturn(initResponse)
                   .thenReturn(initResponseAfterRestart);
            PythonResponse response = new PythonResponse();
            response.setCommand(Command.ACK);
            Mockito.when(process.readResponse()).thenReturn(null);// reader can't read message since writer failed
            WorkMessage input = new WorkMessage();
            WorkMessage inputAfterRestart = new WorkMessage();
            Mockito.doThrow(new IOException("writer throws io exception"))
                   .doNothing()
                   .when(process)
                   .writeMessage(Mockito.any());
            pythonWorker.start();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Assert.assertTrue(pythonWorker.supportsTask("a"));
            Assert.assertTrue(pythonWorker.supportsTask("b"));
            Assert.assertTrue(pythonWorker.supportsTask("c"));
            Assert.assertFalse(pythonWorker.supportsTask("d"));
            Assert.assertEquals(1, pythonWorker.getSubpid().intValue());
            Thread readerThread = pythonWorker.getReaderThread();
            Assert.assertNotNull(readerThread);
            Thread writerThread = pythonWorker.getWriterThread();
            Assert.assertNotNull(writerThread);
            try {
                PythonResponse pythonResponse = pythonWorker.executePython(input);
                if (pythonResponse != null) {
                    Assert.fail("response should be here");
                }
            } catch (PythonProcessException e) {
                Assert.assertEquals(e.getClass(), PythonProcessRestartedException.class);
            }
            Assert.assertFalse(readerThread.isAlive());
            Assert.assertFalse(writerThread.isAlive());

            Mockito.verify(processCleanup, Mockito.times(1)).killProcess(1);
            Mockito.when(process.readResponse()).thenReturn(response);// after restart reader gets response

            PythonResponse pythonResponse = pythonWorker.executePython(inputAfterRestart);
            Assert.assertEquals(pythonResponse, response);
            Assert.assertTrue(pythonWorker.supportsTask("a"));
            Assert.assertFalse(pythonWorker.supportsTask("b"));
            Assert.assertTrue(pythonWorker.supportsTask("c"));
            Assert.assertTrue(pythonWorker.supportsTask("d"));
            Assert.assertEquals(2, pythonWorker.getSubpid().intValue());
            Assert.assertFalse(readerThread == pythonWorker.getReaderThread());
            Assert.assertFalse(writerThread == pythonWorker.getWriterThread());
        } finally {
            pythonWorker.stop();
        }
    }
}
