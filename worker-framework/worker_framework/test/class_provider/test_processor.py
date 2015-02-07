from worker_framework.processors.base_processor import BaseProcessor

class TestProcessor(BaseProcessor):
    '''
    used for python unit tests in worker-framework & for java unit-test to check protocol between java wrapper & python  
    '''
    def process(self, work_message):
        return [work_message]
