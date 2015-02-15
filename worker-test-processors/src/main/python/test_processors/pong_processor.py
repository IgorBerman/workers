from worker_framework.processors.base_processor import BaseProcessor

class PongProcessor(BaseProcessor):    
    def process(self, work_message):
        return []
