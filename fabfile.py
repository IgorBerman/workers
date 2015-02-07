from __future__ import with_statement
from fabric.api import local, settings, abort, run, cd
from fabric.contrib.console import confirm
from fabric.api import *
import os

if os.name == 'nt':
	env.hosts = ['192.168.33.10']
	env.user = 'vagrant'
	env.key_filename = '.vagrant/machines/default/virtualbox/private_key'

def deploy():
	code_dir = '/u/apps/workers'
	sudo("mkdir -p %s " % code_dir)
	sudo("chown vagrant:vagrant %s " % code_dir)
	put('worker-framework-main/build/distributions/worker-framework-main.tar', code_dir)
	with cd(code_dir):
		run('tar -xvf worker-framework-main.tar')
		run('chmod u+x worker-framework-main/worker.sh')
        