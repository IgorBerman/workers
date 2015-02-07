package com.worker.framework.python;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.tenant.framework.CurrentTenant;
import com.worker.framework.python.PythonResponse.Command;
import com.worker.shared.WorkMessage;


@Component
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
// this worker is 1-1 for python process
/**
 * Since thread scope not supports destroy hooks, the processes will be destroyed by special cleaner @link(ShellProcessesCleanup)
 */
public class PerThreadPythonWorker {
    static final int RABBITMQ_FRAME_MAX = 131072;
    private static final int WRITER_TIMEOUT = 5000;
    private static final int READER_TIMEOUT = 5000;

    private static final String WRITER_TO_PROCESS = "WriterToProcess";

    private static final String READER_FROM_PROCESS = "ReaderFromProcess";

    private static final Logger logger = LoggerFactory.getLogger(PerThreadPythonWorker.class);

    @Inject private Provider<ShellProcess> processProvider;
    @Inject private CurrentTenant currentTenant;
    @Inject private PythonWorkerConf pythonWorkerConf;
    @Inject private ShellProcessesCleanup processCleanup;

    private AtomicReference<Throwable> readerOrWriterSystemException = new AtomicReference<Throwable>();
    private SynchronousQueue<PythonWorkMessage> pendingWrites = new SynchronousQueue<PythonWorkMessage>();
    private SynchronousQueue<PythonResponse>    pendingReads  = new SynchronousQueue<PythonResponse>();
    private ShellProcess process;

    private StoppableThread readerThread;
    private StoppableThread writerThread;
    private Number subpid;
    private List<String> supportedTasks;

    @PostConstruct
    public void start() {
        logger.info("Starting python subprocess");
        try {
            process = processProvider.get();
            PythonWorkerInitResponse initResponse = process.launch(pythonWorkerConf);
            if (!Strings.isNullOrEmpty(initResponse.getMsg())) {
                throw new PythonProcessException("Launched subprocess with msg: " + initResponse.getMsg());
            }
            subpid = initResponse.getPid();
            supportedTasks = initResponse.getSupportedTasks();
            logger.info("Launched subprocess with pid " + subpid + " and supported tasks: " + supportedTasks.toString());
            
            startReader();
            startWriter();
            
        } catch (IOException e) {
            logger.error("Problem in starting python sub process", e);
            throw new PythonProcessException("Error when launching multilang subprocess\n" + process.getErrorsString(), e);
        }
    }
    
    private class ReaderThread extends StoppableThread {
        
        ReaderThread(StoppableRunnable target, String name) {
            super(target, name);
        }
    }
    private class WriterThread extends StoppableThread {
        
        WriterThread(StoppableRunnable target, String name) {
            super(target, name);
        }
    }

    private void startReader() {
        // reader
        readerThread = new ReaderThread(new StoppableRunnable() {

            @Override
            public void run() {
                while (isRunning()) {
                    try {

                        PythonResponse pythonResponse = process.readResponse();
                        if (pythonResponse == null) {
                            continue;
                        }
                        //we use offer to be responsive to restarts
                        while (!pendingReads.offer(pythonResponse, READER_TIMEOUT, TimeUnit.MILLISECONDS)) {
                            if(!isRunning()) {
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        logger.error("Problem in reading python response", t);
                        needToBeRestarted(t);
                        break;
                    }
                }
                logger.info("exiting..");
            }
        }, READER_FROM_PROCESS + subpid);

        readerThread.start();
    }

    private void startWriter() {
        writerThread = new WriterThread(new StoppableRunnable() {
            @Override
            public void run() {
                while (isRunning()) {
                    try {
                        PythonWorkMessage write = pendingWrites.poll(WRITER_TIMEOUT, TimeUnit.MILLISECONDS);
                        if (write != null) {
                            process.writeMessage(write);
                        }
                        // drain the error stream to avoid dead lock because of full error stream buffer
                        process.drainErrorStream();
                    } catch (Throwable t) {
                        logger.error("Problem in writing to python subprocess", t);
                        needToBeRestarted(t);
                        break;
                    }
                }
                logger.info("exiting..");
            }
        }, WRITER_TO_PROCESS + subpid);
        writerThread.start();
    }

    public boolean supportsTask(String taskName) {
        String processorName = taskName;
        return supportedTasks.contains(processorName);
    }

    private void needToBeRestarted(Throwable exception) {
        logger.info("setting restarting exception");
        if (this.readerOrWriterSystemException.get() == null) {            
            this.readerOrWriterSystemException.set(exception);
        }
    }

    public PythonResponse executePython(WorkMessage input) throws PythonProcessRestartedException {
        writeMessageToShellProcess(input);
        return readResponseFromShellProcess();
    }

    private void writeMessageToShellProcess(WorkMessage input) {
        PythonWorkMessage pythonMessage = new PythonWorkMessage(currentTenant.get(), input);
        try {
            do {
                restartAndThrowException();//might be something happend
            } while (!pendingWrites.offer(pythonMessage,WRITER_TIMEOUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {//somebody wants java worker to die
            throw new PythonProcessException(e);
        }        
    }    
    
    private PythonResponse readResponseFromShellProcess() {
        while (true) {
            restartAndThrowException();                
            PythonResponse response = null;
            try {
                response = pendingReads.poll(READER_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {//somebody wants java worker to die
                throw new PythonProcessException(e);
            }
            //on timeout
            if (response == null) {
                continue;
            }
            Command command = response.getCommand();
            if (command == Command.ACK) {
                if (response.getTriggeredTasks() == null) {
                    response.setTriggeredTasks(new ArrayList<WorkMessage>());
                }
            } else if (command == Command.ERROR) {
                String msg = response.getMsg();
                int maxLength = (int)(2.0*RABBITMQ_FRAME_MAX/3.0);
                if (!Strings.isNullOrEmpty(msg) && msg.length() >= maxLength) {
                    msg = msg.substring(0, maxLength);
                }
                String errorMessage = "Shell Process Exception in process " + subpid + " : " + msg;
                throw new PythonProcessProcessorException(errorMessage);
            }
            return response;
        }
    }

    /**
     * this method cleans the internal state and restarts subprocess
     * then throws exception to rerun the current message
     */
    private void restartAndThrowException() throws PythonProcessRestartedException {  
        Throwable throwable = readerOrWriterSystemException.get();
        if (throwable == null) {
            return;
        }
        logger.info("Restaring python sub process, due to " + throwable);
        stop();
        start();
        throw new PythonProcessRestartedException("python process restarted", throwable);
    }

    void stop() {
        logger.info("Stoping..");
        stopReader();
        stopWriter();

        process.destroy();
        processCleanup.killProcess(subpid);//cleanup logs/pid files etc
        readerOrWriterSystemException.set(null);
    }

    private void stopWriter() {
        logger.info("Stopping writer..");
        writerThread.requestStop();
        try {
            if (writerThread.isAlive()) {
                writerThread.join();
            }
        } catch (InterruptedException e) {
            throw new PythonProcessException("thread is interupted");
        }
        writerThread = null;
        pendingWrites.clear();
        logger.info("Writer stopped");
    }

    private void stopReader() {
        logger.info("Stoping reader..");
        readerThread.requestStop();
        try {
            if (readerThread.isAlive()) {
                readerThread.join();
            }
        } catch (InterruptedException e) {
            throw new PythonProcessException("thread is interupted");
        }
        readerThread = null;
        pendingReads.clear();
        logger.info("Reader stopped");
    }

    Number getSubpid() {
        return subpid;
    }

    StoppableThread getReaderThread() {
        return readerThread;
    }
    StoppableThread getWriterThread() {
        return writerThread;
    }
    
    class StoppableThread extends Thread {
        private StoppableRunnable target;
        public StoppableThread() {
            super();
        }
        public StoppableThread(StoppableRunnable target, String name) {
            super(target, name);
            this.target = target;
            setDaemon(true);
        }
        public void requestStop() {
            target.requestStop();
        }
    }
    
    abstract class StoppableRunnable implements Runnable {
        private volatile boolean running = true;
        public void requestStop() {
            running = false;
        }
        public boolean isRunning() {
            return running;
        }
    }
}
