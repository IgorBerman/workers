package com.worker.framework.python;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;


@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ShellProcess {

    public static Logger logger = LoggerFactory.getLogger(ShellProcess.class);

    @Inject private ObjectMapper objectMapper;

    private DataOutputStream processIn;
    private BufferedReader processOut;
    private InputStream processErrorStream;
    private Process subprocess;

    public PythonWorkerInitResponse launch(PythonWorkerConf pythonWorkerConf) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(pythonWorkerConf.getCommand());
        builder.directory(new File(pythonWorkerConf.getCodeDir()));
        builder.environment().put("PYTHONPATH", pythonWorkerConf.getCodeDir());
        try {
            subprocess = builder.start();
            processIn = new DataOutputStream(subprocess.getOutputStream());
            processOut = new BufferedReader(new InputStreamReader(subprocess.getInputStream()));
            processErrorStream = subprocess.getErrorStream();
            
            drainErrorStream();
            writeMessage(pythonWorkerConf);
    
            return readInitMessage();
        } catch (IOException e) {
            handleProcessInitializationException(builder);
            throw e;
        }
    }

    private void handleProcessInitializationException(ProcessBuilder builder) {
        logger.error("------------------------------------------");
        logger.error("Launching python subprocess has failed:");
        logger.error("Command: " + builder.command());
        logger.error("directory: " + builder.directory());
        logger.error("environment: " + builder.environment());
        logger.error("------------------------------------------");
        try {
            Thread.sleep(1000);//give it some time to see problems in error stream
        } catch (InterruptedException e1) {
            logger.trace("Interrupted exception", e1);
        }
        drainErrorStream();
    }

    public void destroy() {
        subprocess.destroy();        
    }

    public void writeMessage(Object msg) throws IOException {
        String jsonString = objectMapper.writeValueAsString(msg);
        logger.debug("Writing to python: " + jsonString);
        writeString(jsonString);
    }

    private void writeString(String str) throws IOException {
        byte[] strBytes = str.getBytes("UTF-8");
        processIn.write(strBytes, 0, strBytes.length);
        processIn.writeBytes("\nend\n");
        processIn.flush();
    }

    private PythonWorkerInitResponse readInitMessage() throws IOException {
        PythonWorkerInitResponse response = objectMapper.readValue(readRawMessage(), PythonWorkerInitResponse.class);
        return response;
    }

    public PythonResponse readResponse() throws IOException {
        String readRawMessage = readRawMessage();
        return objectMapper.readValue(readRawMessage, PythonResponse.class);
    }
    private String readRawMessage() throws IOException {
        String jsonString = readString();
        logger.debug("Got from python: " + jsonString);
        return jsonString;
    }

    public String getErrorsString() {
        if (processErrorStream != null) {
            try {
                return IOUtils.toString(processErrorStream);
            } catch (IOException e) {
                logger.error("Problem in processing error stream", e);
                return "(Unable to capture error stream)";
            }
        } else {
            return "";
        }
    }

    public void drainErrorStream() {
        if (processErrorStream != null) {
            try {
                while (processErrorStream.available() > 0) {
                    int bufferSize = processErrorStream.available();
                    byte[] errorReadingBuffer = new byte[bufferSize];
    
                    processErrorStream.read(errorReadingBuffer, 0, bufferSize);
    
                    logger.error("Got error from shell process: " + new String(errorReadingBuffer));
                }
            } catch (Exception e) {
                logger.error("problem while draining errors ", e);
            }
        }
    }

    private String readString() throws IOException {
        StringBuilder line = new StringBuilder();

        while (true) {
            String subline = processOut.readLine();
            if (subline == null) {
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Pipe to subprocess seems to be broken!");
                if (line.length() == 0) {
                    errorMessage.append(" No output read.\n");
                } else {
                    errorMessage.append(" Currently read output: " + line.toString() + "\n");
                }
                errorMessage.append("Shell Process error:\n");
                errorMessage.append("'" + getErrorsString() + "'\n");
                logger.error("Problem in python process", errorMessage);                
                throw new ShellProcessException(errorMessage.toString());
            }
            if (subline.equals("end")) {
                break;
            }
            if (line.length() != 0) {
                line.append("\n");
            }
            line.append(subline);
        }

        return line.toString();
    }
    void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
