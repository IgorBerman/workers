import time
from worker_framework.processors.base_processor import BaseProcessor
from worker_framework.worker.work_message import WorkMessageArg, WorkMessage


class DependencyTreeProcessor(BaseProcessor):
    """
    processor that creates binary tree with root # 1 up to # 2^tree_height - 1 
    root at level 1 and leaves at level tree_height
    default height is 4
    args: id, [tree_height], [i need to fail]
    """
    def process(self, work_message):
        my_number = int(work_message.args[0].value)
        tree_height = int(work_message.args[1].value) if len(work_message.args) >= 2 else 4
        i_need_to_fail = int(work_message.args[2].value) if len(work_message.args) >= 3 else my_number - 1
        if (i_need_to_fail == my_number):
            raise ValueError("i need to fail")
        time.sleep(1)
        print ",".join([str(my_number),str(tree_height),str(i_need_to_fail)])
        if my_number <= pow(2, tree_height - 1):
            print 'producing children ',
            left_child = WorkMessage(work_message.task, [WorkMessageArg('my_number', 2*my_number), WorkMessageArg('tree_height', tree_height), WorkMessageArg('i_need_to_fail', i_need_to_fail)])
            right_child = WorkMessage(work_message.task, [WorkMessageArg('my_number', 2*my_number+1), WorkMessageArg('tree_height', tree_height), WorkMessageArg('i_need_to_fail', i_need_to_fail)])
            return [left_child, right_child]
        return []
