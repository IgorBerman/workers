import time
from worker_framework.processors.base_processor import BaseProcessor
from worker_framework.worker.work_message import WorkMessage, WorkMessageArg,\
    WorkMessagesJoinState


class TriggerTaskChainProcessor(BaseProcessor):
    '''
    Used in integration tests, search for TriggerTaskChain
    '''
    def process(self, work_message):
        sinkMessage = WorkMessage("TriggerTaskChainReqursiveProcessor", lowPriority = work_message.lowPriority)
        joinState = WorkMessagesJoinState("SleepingJoinState" + str(int(time.time())), sinkMessage)
        new_message1 = WorkMessage("Sleeping", [WorkMessageArg("time", 1), WorkMessageArg("id", 1)], joinState, lowPriority = work_message.lowPriority)
        new_message2 = WorkMessage("Sleeping", [WorkMessageArg("time", 1), WorkMessageArg("id", 2)], joinState, lowPriority = work_message.lowPriority)
        new_message3 = WorkMessage("Sleeping", [WorkMessageArg("time", 1), WorkMessageArg("id", 3)], joinState, lowPriority = work_message.lowPriority)
        return [new_message1, new_message2, new_message3]
