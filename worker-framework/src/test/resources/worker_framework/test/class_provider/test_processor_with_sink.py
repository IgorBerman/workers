from worker_framework.processors.base_processor import BaseProcessor
from worker_framework.worker.work_message import WorkMessage, WorkMessageArg,\
    WorkMessagesJoinState

class TestWithSinkProcessor(BaseProcessor):
    '''
    used for python unit tests in worker-framework & for java unit-test to check protocol between java wrapper & python  
    '''
    def process(self, work_message):
        sink_message = WorkMessage("bla", [WorkMessageArg("name", 1)])
        response_message = WorkMessage("TestWithSink", [WorkMessageArg("name", 1)], WorkMessagesJoinState("join-id", sink_message))
        response_message.lowPriority=True
        return [response_message]
