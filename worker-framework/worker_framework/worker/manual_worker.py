import logging
import traceback

from analysis_infra.database.connection_holder import CONNECTION_HOLDER
from worker_framework.class_provider.class_provider import ProcessorsProvider, \
    UnsupportedProcessorException
from worker_framework.worker.work_message import WorkMessage, WorkMessageArg
from worker_framework.worker.worker import Worker, DEFAULT_SUB_MODULES_TO_SCAN


class ManualWorker(Worker):

    def __init__(self, sub_modules_to_scan):
        self._logger = logging.getLogger('worker')
        self._logger.setLevel(logging.DEBUG)
        ch = logging.StreamHandler()
        ch.setLevel(logging.DEBUG)
        formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        ch.setFormatter(formatter)
        self._logger.addHandler(ch)
        self.processor_provider = ProcessorsProvider(self._logger, sub_modules_to_scan)
        
    def run(self, tenantId, db_url, wm):
        try:
            self.processor_provider.load()
            CONNECTION_HOLDER.set_connection_urls({tenantId:db_url})
            processor_name = wm.task
            if not self.processor_provider.has_processor(processor_name):
                raise UnsupportedProcessorException(processor_name)
            CONNECTION_HOLDER.update_current_tenant(tenantId)
            return self.processor_provider.get_processor(processor_name).process(wm)
        except Exception, e:
            self._logger.error(traceback.format_exc(e))

if __name__ == '__main__':
    """
    Used for debug purposes
    """
    worker = ManualWorker(DEFAULT_SUB_MODULES_TO_SCAN)
    tenantId = 'sww'
    db_url = "postgresql://%s:%s@%s/%s_db" % (tenantId, tenantId, "localhost", tenantId)
    task = "NeighboringSubnetsEventAnalysis"     
    args = [WorkMessageArg(name="network_id", value=62400622), WorkMessageArg(name="end_time_bound", value=1410595320000)]
    joinState = None
    wm = WorkMessage(task, args, joinState)
    worker.run(tenantId, db_url, wm)