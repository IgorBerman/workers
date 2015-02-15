from worker_framework.processors.base_processor import BaseProcessor
from worker_framework.worker.work_message import WorkMessage


class PingProcessor(BaseProcessor):    
    def process(self, work_message):
        new_message = WorkMessage("PongProcessor", work_message.args)
        return [new_message]
