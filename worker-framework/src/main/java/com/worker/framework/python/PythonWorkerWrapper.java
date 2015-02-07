package com.worker.framework.python;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.worker.framework.api.Worker;
import com.worker.shared.WorkMessage;


@Component
public class PythonWorkerWrapper implements Worker {
    
    @Inject PerThreadPythonWorker perThreadPythonWorker;

    public List<WorkMessage> execute(WorkMessage input) {
        return perThreadPythonWorker.executePython(input).getTriggeredTasks();
    }

    @Override
    public boolean supportsTask(String taskName) {
        return perThreadPythonWorker.supportsTask(taskName);
    }    
}
