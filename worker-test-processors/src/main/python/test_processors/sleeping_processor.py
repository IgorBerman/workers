import time
from worker_framework.processors.base_processor import BaseProcessor


class SleepingProcessor(BaseProcessor):
    
    def process(self, work_message):
        sleep_time = int(work_message.args[0].value)
        print 'sleeping for ', sleep_time 
        time.sleep(sleep_time)
        return []
