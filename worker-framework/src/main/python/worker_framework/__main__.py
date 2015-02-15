import sys
from worker_framework.worker.worker import Worker
from worker_framework.worker.worker import DEFAULT_LOG_PATH,\
    DEFAULT_SUB_MODULES_TO_SCAN

if __name__ == '__main__':
    log_path = sys.argv[1] if len(sys.argv) == 2 else DEFAULT_LOG_PATH
    worker = Worker(log_path, DEFAULT_SUB_MODULES_TO_SCAN)
    worker.run()
