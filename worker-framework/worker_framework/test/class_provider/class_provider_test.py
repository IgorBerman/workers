import unittest

from worker_framework.class_provider.class_provider import ProcessorsProvider, \
    UnsupportedProcessorException
from worker_framework.processors.base_processor import BaseProcessor

class DummyLogger():
    def warn(self, message):
        print str(message)
    def info(self, message):
        print str(message)
    def exception(self, message):
        print str(message)
        
class ProcessorProviderTest(unittest.TestCase):
    
    EXAMPLE_PROCCESOR = "TestProcessor"
    
    def __init__(self, *args, **kwargs):
        super(ProcessorProviderTest, self).__init__(*args, **kwargs)
        logger = DummyLogger()
        self.provider = ProcessorsProvider(logger, ['worker_framework'])
        
    def test_has_processor(self):
        self.provider.load()
        self.assertFalse(self.provider.has_processor("some non existing processor name"))
        self.assertTrue(self.provider.has_processor(self.EXAMPLE_PROCCESOR))

    def test_get_processor(self):
        self.provider.load()
        processor = self.provider.get_processor(self.EXAMPLE_PROCCESOR)
        self.assertTrue(isinstance(processor, BaseProcessor))
    
    def test_get_invalid_processor(self):
        self.provider.load()
        self.assertRaises(UnsupportedProcessorException, self.provider.get_processor, "some non existing processor name")
