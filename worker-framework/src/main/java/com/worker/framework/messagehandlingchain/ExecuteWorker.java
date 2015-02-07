package com.worker.framework.messagehandlingchain;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.worker.framework.api.Worker;
import com.worker.shared.WorkMessage;

public class ExecuteWorker implements WMProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ExecuteWorker.class);

    private final List<Worker> workers;

    public ExecuteWorker(List<Worker> workers) {
        this.workers = workers;
    }

    @Override
    public List<WorkMessage> handle(WorkMessage input) throws Exception {
        String task = input.getTask();
        for (Worker worker : workers) {
            if (worker.supportsTask(task)) {
                return worker.execute(input);
            }
        }
        logger.error("Can't find processor for " + task);
        throw new IllegalArgumentException("No processor found for given task: " + task);
    }
}