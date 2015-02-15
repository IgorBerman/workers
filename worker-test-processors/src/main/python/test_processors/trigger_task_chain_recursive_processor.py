import time

from worker_framework.processors.base_processor import BaseProcessor
from worker_framework.worker.work_message import WorkMessage, WorkMessageArg, \
    WorkMessagesJoinState


class TriggerTaskChainReqursiveProcessor(BaseProcessor):
    '''
    Used in integration tests, search for TriggerTaskChainReqursive
    '''   
    def process(self, work_message):
        low_priority = work_message.lowPriority
        it_num = work_message.args[0].value if work_message.args is not None and len(work_message.args) >=1 else 2
        join_state = None
        if it_num > 0:
            next_it_num = it_num - 1
            sink_message = WorkMessage("TriggerTaskChainReqursiveProcessor", [WorkMessageArg("it_num", next_it_num)], lowPriority=low_priority)
            join_state = WorkMessagesJoinState("SleepJoinState" + str(int(time.time())), sink_message)
        new_message1 = WorkMessage("Sleeping", [WorkMessageArg("time", 1), WorkMessageArg("id", 1)], join_state, lowPriority=low_priority)
        new_message2 = WorkMessage("Sleeping", [WorkMessageArg("time", 1), WorkMessageArg("id", 2)], join_state, lowPriority=low_priority)
        return [new_message1, new_message2]
