from __future__ import with_statement
from fabric.api import local, settings, abort, run, cd
from fabric.contrib.console import confirm
from fabric.api import *
import os

def deploy():
	code_dir = '/u/apps/workers'
	sudo("mkdir -p %s " % code_dir)
	sudo("chown vagrant:vagrant %s " % code_dir)
	put('worker-framework-main/build/distributions/worker-framework-main.tar', code_dir)
	with cd(code_dir):
		run('tar -xvf worker-framework-main.tar')
		run('chmod u+x worker-framework-main/worker.sh')

def provision():
	install_java8()
	
def install_java8():
	'''
	http://www.webupd8.org/2014/03/how-to-install-oracle-java-8-in-debian.html
	'''
	sudo('echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list')
	sudo('echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list')
	sudo('apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886')
	sudo('apt-get update')
	sudo('echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections')
	sudo('apt-get install -y oracle-java8-installer')
	sudo('sudo apt-get -y install oracle-java8-set-default')
	
        