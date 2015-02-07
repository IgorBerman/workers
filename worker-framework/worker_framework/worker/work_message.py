# Worker framework DTOs. Could not use namedtuple as json encoder does not support in a simple way.
import uuid

class WorkMessage(object):
    def __init__(self, task, args=[], joinState=None, lowPriority=False):
        self.task = task
        self.args = args
        self.joinState = joinState
        self.lowPriority = lowPriority
        self.uuid = uuid.uuid4().get_hex()#it's very important that every message will be with unique uuid! don't copy it from anywhere
        
    def __eq__(self, other):
        return (isinstance(other, self.__class__) and self.__dict__ == other.__dict__)
    def __ne__(self, other):
        return not self.__eq__(other)
    def __repr__(self):
        return str(self.__dict__)

class WorkMessageArg(object):
    def __init__(self, name, value):
        self.name = name
        self.value = value
        
    def __eq__(self, other):
        return (isinstance(other, self.__class__) and self.__dict__ == other.__dict__)
    
    def __ne__(self, other):
        return not self.__eq__(other)

    def __repr__(self):
        return str(self.__dict__)
    
class WorkMessagesJoinState(object):
    def __init__(self, joinId, sinkMessage):
        self.joinId = joinId
        self.sinkMessage = sinkMessage 
               
    def __eq__(self, other):
        return (isinstance(other, self.__class__) and self.__dict__ == other.__dict__)
    
    def __ne__(self, other):
        return not self.__eq__(other)

    def __repr__(self):
        return str(self.__dict__)
    
class InvalidWorkMessage(RuntimeError):
    pass
