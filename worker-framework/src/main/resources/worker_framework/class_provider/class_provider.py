import importlib
import inspect
import os
import pkgutil
import sys

from worker_framework.processors.base_processor import BaseProcessor


class ProcessorsProvider(object):
    
    def __init__(self, logger, modules_to_scan):
        self.class_loader = ClassLoader(logger,modules_to_scan)
        self.processors = {}
    
    def load(self):
        self.processors = self.class_loader.load(BaseProcessor)
    
    def has_processor(self, processor_name):
        return self.processors.has_key(processor_name)
    
    def get_processor(self, processor_name):
        if self.has_processor(processor_name):
            return self.processors.get(processor_name)
        raise UnsupportedProcessorException(processor_name)
    
    def get_all_processor_names(self):
        return self.processors.keys()

class UnsupportedProcessorException(Exception):
    pass

class ClassLoader(object):
    
    def __init__(self, logger,modules_to_scan):
        self._logger = logger
        self._modules_to_scan = modules_to_scan
        self._current_module = os.path.splitext(os.path.basename(__file__))[0]    
    
    def _is_applicable(self, base_class, cls):
        return inspect.isclass(cls) \
            and issubclass(cls, base_class) \
            and cls.__name__ != base_class.__name__ \
            and not inspect.isabstract(cls)

    def _hide_stdout(self):
        self._stderr = sys.stderr
        self._stdout = sys.stdout
        null = open(os.devnull,'wb')
        sys.stderr = sys.stdout = null

    def _return_stdout(self):
        sys.stderr = self._stderr
        sys.stdout = self._stdout


    def _skip_package(self, modname):
        return not any(map(lambda x:x in modname, self._modules_to_scan)) or modname == self._current_module or modname.startswith("analysis_research")

    def load(self, base_class):
        self._hide_stdout()
        objects = {}
        for _, modname, _ in pkgutil.walk_packages(onerror=lambda package_name : self._logger.warn("problem trying to traverse package %s, it's not a problem if there are no processors" % package_name)): 

            if self._skip_package(modname):
                continue
            
            if self._logger:
                self._logger.info("scanning " + modname)
            try:
                module = importlib.import_module(modname)
                members = inspect.getmembers(module)
                
                for name, cls in members: 
                    if name not in objects and self._is_applicable(base_class, cls): 
                        if self._logger:
                            self._logger.info("registering " + str(cls))                            
                        objects[name] = cls() # Store a mapping from class name to instance
            except Exception, e:
                #pass
                if self._logger:
                    self._logger.exception(e)   

        self._return_stdout()
        return objects

