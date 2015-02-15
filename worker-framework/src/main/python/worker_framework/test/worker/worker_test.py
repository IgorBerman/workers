from StringIO import StringIO
import re
import shutil
import sys
import tempfile
import unittest

from mock import create_autospec, patch, Mock
import mock

from analysis_infra.database.connection_holder import ConnectionHolder
from worker_framework.class_provider.class_provider import ProcessorsProvider
from worker_framework.processors.base_processor import BaseProcessor
from worker_framework.processors.processor_task_name_resolver import ProcessorTaskNameResolver
from worker_framework.worker.work_message import WorkMessage, WorkMessageArg, \
    WorkMessagesJoinState
from worker_framework.worker.worker import DEFAULT_LOG_PATH, DEFAULT_SUB_MODULES_TO_SCAN, \
    java_python_tokens, json_encode
import  worker_framework.worker.worker as worker_module


class WorkerTest(unittest.TestCase):
    
    def testInitialize(self):
        worker = worker_module.Worker(DEFAULT_LOG_PATH, DEFAULT_SUB_MODULES_TO_SCAN)
        old_stdin = sys.stdin
        old_stdout = sys.__stdout__
        
        mock_processor_provider = create_autospec(ProcessorsProvider)
        mock_processor_provider.get_all_processor_names.return_value = ['processor1', 'processor2'] 
        worker.processor_provider = mock_processor_provider
        
        mock_processor_task_name_resolver = create_autospec(ProcessorTaskNameResolver)
        mock_processor_task_name_resolver.get_procssor_name.side_effect = ['processor1Processor', 'processor2Processor'] 
        worker.processor_task_name_resolver = mock_processor_task_name_resolver
        
        pid_dir = tempfile.mkdtemp()
        
        sys.stdin = StringIO('{"codeDir":".","%s":["python","some command"],"%s":[{"tenantId":"mytenant","dbUrl":"dburl"}],"%s":"%s"}\n%s\n'
                                % (java_python_tokens.COMMAND, java_python_tokens.CONNECTION_URLS, java_python_tokens.PID_DIR, pid_dir, java_python_tokens.END))
        out = StringIO()
        sys.__stdout__ = out
        
        worker._initialize(True)

        sys.stdin = old_stdin
        sys.__stdout__ = old_stdout

        regex = re.compile(r'^\{"%s":\d+,"%s":\["processor1","processor2"\]\}\n%s\n$'%\
                           (java_python_tokens.PID, java_python_tokens.SUPPORTED_TASKS, java_python_tokens.END))
        self.assertTrue(re.match(regex, out.getvalue()) is not None)
        
        shutil.rmtree(pid_dir)

    def testReadWorkMessageAndProcess(self):
        worker = worker_module.Worker(DEFAULT_LOG_PATH, DEFAULT_SUB_MODULES_TO_SCAN)
        old_stdin = sys.stdin
        old_stdout = sys.__stdout__
        
        worker_module.CONNECTION_HOLDER = create_autospec(ConnectionHolder)
        processor_mock = create_autospec(BaseProcessor)
        work_message = WorkMessage("triggered task 1", [WorkMessageArg("arg11", "arg11 value"), WorkMessageArg("arg12", "arg12 value")], lowPriority=True)
        processor_mock.process.return_value = [work_message] 
        mock_processor_provider = create_autospec(ProcessorsProvider)
        mock_processor_provider.get_processor.return_value = processor_mock
        worker.processor_provider = mock_processor_provider
        
        sys.stdin = StringIO('{"%s":"indaqua","%s":{"%s":"Test","%s":[{"%s":"arg1","%s":"arg1 value"},{"%s":"arg2","%s":"arg2 value"}],"lowPriority":true}}\n%s\n'%\
                             (java_python_tokens.TENANT_ID, java_python_tokens.WORK_MESSAGE, java_python_tokens.TASK, java_python_tokens.ARGS, java_python_tokens.NAME, java_python_tokens.VALUE,\
                             java_python_tokens.NAME, java_python_tokens.VALUE, java_python_tokens.END))
        out = StringIO()
        sys.__stdout__ = out
        
        worker._readWorkMessageAndProcess()

        sys.stdin = old_stdin
        sys.__stdout__ = old_stdout
        
        wm_as_json = json_encode([work_message])
        expected_output = '{"%s":%s,"%s":"%s"}\n%s\n'%\
            (java_python_tokens.TRIGGERED_TASKS, wm_as_json, java_python_tokens.COMMAND, java_python_tokens.ACK, java_python_tokens.END)
            
        self.assertEquals(expected_output, out.getvalue())

    @patch('worker_framework.worker.work_message.uuid')
    def test_json_decode_full_message(self, uuid_mock):
        uuid4_mock = Mock()
        uuid4_mock.get_hex.return_value = "1"
        uuid_mock.uuid4.return_value = uuid4_mock
        worker = worker_module.Worker(DEFAULT_LOG_PATH, DEFAULT_SUB_MODULES_TO_SCAN)
        
        worker_module.CONNECTION_HOLDER = create_autospec(ConnectionHolder)
        processor_mock = create_autospec(BaseProcessor)
        processor_mock.process.return_value = []
        mock_processor_provider = create_autospec(ProcessorsProvider)
        mock_processor_provider.get_processor.return_value = processor_mock
        worker.processor_provider = mock_processor_provider
        
        sink_message = WorkMessage("SinkMessage", [WorkMessageArg("arg1", 1.0), WorkMessageArg("arg2", "bla")])
        joinState = WorkMessagesJoinState("joinId", sink_message)
        wm = WorkMessage("Test", [WorkMessageArg("arg3", 1.0), WorkMessageArg("arg4", "bla")], joinState=joinState, lowPriority=True)        
        work_message_as_json = json_encode(wm)
        input_as_json = '{"%s":"indaqua","%s":%s}\n%s\n'%\
                             (java_python_tokens.TENANT_ID,  java_python_tokens.WORK_MESSAGE, work_message_as_json, java_python_tokens.END)
        try:
            old_stdin = sys.stdin
            sys.stdin = StringIO(input_as_json)
            
            worker._readWorkMessageAndProcess()
        finally:
            sys.stdin = old_stdin
            
        work_message_arg =  processor_mock.process.call_args[0][0]
        
        self.assertEqual(work_message_arg, wm)
        
    @patch('worker_framework.worker.work_message.uuid')
    def test_json_decode_no_join_message(self, uuid_mock):
        uuid4_mock = Mock()
        uuid4_mock.get_hex.return_value = "1"
        uuid_mock.uuid4.return_value = uuid4_mock
        
        worker = worker_module.Worker(DEFAULT_LOG_PATH, DEFAULT_SUB_MODULES_TO_SCAN)
        
        worker_module.CONNECTION_HOLDER = create_autospec(ConnectionHolder)
        processor_mock = create_autospec(BaseProcessor)
        processor_mock.process.return_value = []
        mock_processor_provider = create_autospec(ProcessorsProvider)
        mock_processor_provider.get_processor.return_value = processor_mock
        worker.processor_provider = mock_processor_provider
        
        wm = WorkMessage("Test", [WorkMessageArg("arg3", 1.0), WorkMessageArg("arg4", "bla")])        
        work_message_as_json = json_encode(wm)
        input_as_json = '{"%s":"indaqua","%s":%s}\n%s\n'%\
                             (java_python_tokens.TENANT_ID,  java_python_tokens.WORK_MESSAGE, work_message_as_json, java_python_tokens.END)
        try:
            old_stdin = sys.stdin
            sys.stdin = StringIO(input_as_json)
            
            worker._readWorkMessageAndProcess()
        finally:
            sys.stdin = old_stdin
            
        work_message_arg =  processor_mock.process.call_args[0][0]
        
        self.assertEqual(work_message_arg, wm)

