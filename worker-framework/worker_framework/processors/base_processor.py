from abc import ABCMeta, abstractmethod
    
class BaseProcessor(object):
    __metaclass__ = ABCMeta
    
    @abstractmethod
    def process(self, work_message):
        """
        Perform necessary process on the given process message and return triggered tasks
        """
        raise NotImplementedError
