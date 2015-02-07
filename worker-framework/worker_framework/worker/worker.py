import logging.handlers
import os
from select import select
import sys
import threading
import traceback

from worker_framework.class_provider.class_provider import ProcessorsProvider, \
    UnsupportedProcessorException
from worker_framework.worker.work_message import WorkMessage, WorkMessageArg, \
    WorkMessagesJoinState


#if module contains one of sub modules it will be scanned
DEFAULT_SUB_MODULES_TO_SCAN = ['worker_framework']
DEFAULT_LOG_PATH = '/var/log/python-worker-%s.log'
DEFAULT_LOG_COUNT = 10
DEFAULT_LOG_MAX_BYTES = 10000000
WORKER_LOGGER_NAME = 'worker'

json_encode = lambda x: json.dumps(x, cls=ExtendedEncoder, ensure_ascii=False, separators=(',', ':'))
json_decode = lambda x: json.loads(x)

class java_python_tokens:
    ARGS = 'args'
    VALUE = 'value'
    NAME = 'name'
    TASK = 'task'
    JOIN_STATE = 'joinState'
    JOIN_ID = 'joinId'
    SINK_MESSAGE = 'sinkMessage'
    LOW_PRIORITY = 'lowPriority'
    TENANT_ID = "tenantId"
    WORK_MESSAGE = 'workMessage'
    SUPPORTED_TASKS = "supportedTasks"
    PID = "pid"
    TRIGGERED_TASKS = "triggeredTasks"
    PID_DIR = 'piddir'
    COMMAND = "command"
    ACK = "ACK"
    ERROR = "ERROR"
    MSG = "msg"
    END = "end"
    CONNECTION_URLS = 'connectionUrls'
    DB_URL = 'dbUrl'
    
class StreamToLogger(object):
    """
    Fake file-like stream object that redirects writes to a logger instance.
    """
    def __init__(self, logger, log_level=logging.DEBUG):
        self.logger = logger
        self.log_level = log_level
        self.linebuf = ''
 
    def write(self, buf):
        for line in buf.rstrip().splitlines():
            self.logger.log(self.log_level, line.rstrip())

class Worker(object):

    def __init__(self, log_file_path, sub_modules_to_scan):        
        self._logger = logging.getLogger(WORKER_LOGGER_NAME)
        self._logger.setLevel(logging.DEBUG)
        fh = logging.handlers.RotatingFileHandler(log_file_path%os.getpid(), maxBytes=DEFAULT_LOG_MAX_BYTES, backupCount=DEFAULT_LOG_COUNT)
        fh.setLevel(logging.DEBUG)
        formatter = logging.Formatter(" $ ".join([
            "%(asctime)s",
            "%(levelname)s",
            "%(filename)s:%(lineno)d",
            "%(funcName)s()",
            "%(message)s"
        ]))
        fh.setFormatter(formatter)
        self._logger.addHandler(fh)
        self._pid = os.getpid()
        self._ppid = os.getppid()
        threading.Timer(60, self._check_parent).start()
        sys.stdout = StreamToLogger(self._logger)
        self.processor_provider = ProcessorsProvider(self._logger, sub_modules_to_scan)
        
    def _check_parent(self):
        if (self._ppid != os.getppid()):
            self._logger.error("parent has been changed, exiting..")
            os._exit(-1)
        
    def _ack(self, workMessage, triggeredTasks):
        ack_response = {java_python_tokens.COMMAND: java_python_tokens.ACK}
        if triggeredTasks is not None:
                ack_response[java_python_tokens.TRIGGERED_TASKS] = triggeredTasks
        self._sendMsgToParent(ack_response)
            
    def _initComponent(self, processor_names):
        self._logger.info("init component")
        setup_info = self._readMsg()
        self._sendInitResponse(setup_info[java_python_tokens.PID_DIR], processor_names)
        return setup_info
    
    def _initialize(self, init_component):
        self._logger.info("loading processors..")
        self.processor_provider.load()
        if init_component:
            setup_info = self._initComponent(self.processor_provider.get_all_processor_names())
        self._logger.info("initializing connections..")
        connention_urls_dict = self._getConnectionUrlsDict(setup_info[java_python_tokens.CONNECTION_URLS])

    def _getConnectionUrlsDict(self, connectionUrlsList):
        conn_dict = {}
        for connection_url in connectionUrlsList:
            conn_dict[connection_url[java_python_tokens.TENANT_ID]] = connection_url[java_python_tokens.DB_URL]
        return conn_dict
    
    def process(self, tenantId, workMessage):
        """
        run suitable processor
        """
        processor_name = self.processor_task_name_resolver.get_procssor_name(workMessage.task)
        if not self.processor_provider.has_processor(processor_name):
            raise UnsupportedProcessorException(processor_name)
        return self.processor_provider.get_processor(processor_name).process(workMessage)

    def _reportError(self, error_msg):
        self._sendMsgToParent({java_python_tokens.COMMAND: java_python_tokens.ERROR, java_python_tokens.MSG: unicode(error_msg)})
    
    #reads lines and reconstructs newlines appropriately
    # stdin is unlikely to be accessed by someone else, so no need to use __stdin__
    def _readMsg(self):
        msg = ""
        line = sys.stdin.readline()[0:-1]
        while line != java_python_tokens.END:
            msg = msg + line + "\n"
            line = sys.stdin.readline()[0:-1]
        self._logger.debug("got from parent " + msg[0:-1])
        x = None
        try :
            x = json_decode(msg[0:-1])
        except Exception, e:
            self._logger.error(e)
        return x
    
    def _parse_work_message_from_json(self, wm_as_dict):
        wm_ctor_dict = {}
        if java_python_tokens.ARGS in wm_as_dict:
            wm_ctor_dict['args'] = [WorkMessageArg(arg[java_python_tokens.NAME], arg[java_python_tokens.VALUE]) for arg in wm_as_dict[java_python_tokens.ARGS]]
        if java_python_tokens.JOIN_STATE in wm_as_dict:
            join_state_as_dict = wm_as_dict[java_python_tokens.JOIN_STATE]
            if join_state_as_dict is not None:
                joinId = join_state_as_dict[java_python_tokens.JOIN_ID]
                sinkMessage = self._parse_work_message_from_json(join_state_as_dict[java_python_tokens.SINK_MESSAGE])
                wm_ctor_dict['joinState'] = WorkMessagesJoinState(joinId, sinkMessage)
        if java_python_tokens.LOW_PRIORITY in wm_as_dict:
            wm_ctor_dict['lowPriority'] = wm_as_dict[java_python_tokens.LOW_PRIORITY]
        return WorkMessage(wm_as_dict[java_python_tokens.TASK], **wm_ctor_dict)

    def _readWorkMessage(self):
        cmd = self._readMsg()
        wm = cmd[java_python_tokens.WORK_MESSAGE]
        worker_message = self._parse_work_message_from_json(wm) 
        return [cmd[java_python_tokens.TENANT_ID], worker_message]
    
    def _readWorkMessageAndProcess(self):
        try:
            tenantId, workMessage = self._readWorkMessage()
            triggeredTasks = self.process(tenantId, workMessage)
            self._ack(workMessage, triggeredTasks)
        except Exception, e:
            self._logger.error(traceback.format_exc(e))
            self._reportError(traceback.format_exc(e))   
    
    def _sendInitResponse(self, pid_dir, processor_names):        
        if not os.path.exists(pid_dir):
            os.makedirs(pid_dir)
        self._sendMsgToParent({java_python_tokens.PID:self._pid, java_python_tokens.SUPPORTED_TASKS: processor_names})
        open(pid_dir + "/" + str(self._pid), "w").close()
    
    def _sendMsgToParent(self, o):
        encoded_msg = json_encode(o)
        self._logger.info("send msg to parent: "+ unicode(encoded_msg))
        print>>sys.__stdout__, encoded_msg.encode('utf-8')        
        print>>sys.__stdout__, java_python_tokens.END
        sys.__stdout__.flush()      
        
    def run(self, init_component = True):
        self._logger.info("starting..")
        try:
            self._initialize(init_component)
            self._logger.info("entering main loop..")
            while True:
                self._readWorkMessageAndProcess()
        except Exception, e:
            self._logger.error(traceback.format_exc(e))
            self._reportError(traceback.format_exc(e))

if __name__ == '__main__':
    log_path = sys.argv[1] if len(sys.argv) == 2 else DEFAULT_LOG_PATH
    worker = Worker(log_path, DEFAULT_SUB_MODULES_TO_SCAN)
    worker.run()
