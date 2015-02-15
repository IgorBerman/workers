package com.worker.framework.python;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.Phased;
import org.springframework.stereotype.Component;

import com.worker.framework.api.WorkerProperties;


@Component
public class ShellProcessesCleanup implements Phased {

    private static final String KILL_9 = "kill -9 ";

    private static final Logger logger = LoggerFactory.getLogger(ShellProcessesCleanup.class);

    @Inject private WorkerProperties properties;

    @PreDestroy
    @PostConstruct
    public void cleanup() {
        logger.info("cleaning old processes");
        File pidDir = new File(properties.getPidDir());
        if (pidDir.exists() && pidDir.isDirectory()) {
            cleanup(pidDir);
        }
        //giving it some time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            logger.trace("Interrupted exception", e);
        }
        logger.info("done cleaning old processes");
    }

    private void cleanup(File pidDir) {
        logger.debug("checking processes' pids in " + pidDir);
        File[] pidFiles = pidDir.listFiles();
        if (pidFiles != null && pidFiles.length > 0) {
            for (File pidFile : pidFiles) {
                killProcessAndDeletePidFile(pidFile);
            }
        } else {
            logger.info("pids dir " + pidDir + " is empty or doens't exists, skipping");
        }
    }

    public void killProcess(Number pid) {
        File pidFile = new File(properties.getPidDir(), String.valueOf(pid));
        killProcessAndDeletePidFile(pidFile);
    }

    private void killProcessAndDeletePidFile(File pidFile) {
        logger.debug("killing " + pidFile.getName());
        String pid = pidFile.getName();
        try {
            Process exec = Runtime.getRuntime().exec(KILL_9 + pid);
            exec.waitFor();
            logger.debug("removing pid file");
            FileUtils.deleteQuietly(pidFile);
            if (properties.isPythonLogsCleanup()) {
                FileUtils.deleteQuietly(new File(String.format(properties.getPythonLogPathPrefix(), pid)));
            }
        } catch (IOException e) {
            logger.error("killing " + pidFile + " failed, please check it manually ", e);
        } catch (InterruptedException e) {
            logger.error("killing " + pidFile + " failed, please check it manually ", e);
        }
    }

    @Override
    public int getPhase() {
        return -100; //should be started before any other bus.logic beans
    }

}
