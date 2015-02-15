from __future__ import with_statement
from fabric.api import local, settings, abort, run, cd
from fabric.contrib.console import confirm
from fabric.api import *
import os
import StringIO
#fab -D -H 192.168.33.10 -u vagrant -i .vagrant/machines/default/virtualbox/private_key install_supervisor

LOG_DIR='/var/log/workers'

def deploy(user="vagrant", group="vagrant"):
    code_dir = '/u/apps/workers'
    sudo("mkdir -p %s " % code_dir)
    sudo("chown %s:%s %s " % (user,group, code_dir))
    put('worker-framework-main/build/distributions/worker-framework-main.tar', code_dir)
    with cd(code_dir):
        new_release_name = run('date +%Y_%m_%d_%H_%M_%S')
        run('mkdir %s' % new_release_name)
        run('tar -xvf worker-framework-main.tar -C %s' % new_release_name)
        run('rm -rf current && ln -s %s current' % new_release_name)
        run('rm -rf `ls -t | tail -n +6`')
        sudo("chown -R %s:%s %s " % (user,group, new_release_name))
        sudo('supervisorctl restart workers')
        
def provision(user="vagrant", group="vagrant"):
    install_java8()
    install_supervisor()
    prepare_for_app(user,group)
    
def install_supervisor():
    sudo('apt-get install -y supervisor')
    put(StringIO.StringIO('''[program:workers]
command=/u/apps/workers/current/worker-framework-main/bin/worker-framework-main
autostart=true
autorestart=true
stderr_logfile = %s/workers-stderr.log
stdout_logfile = %s/workers-stdout.log
''' % (LOG_DIR,LOG_DIR))\
    , '/etc/supervisor/conf.d/workers.conf',use_sudo=True)
    
    
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
    
def prepare_for_app(user,group):
    sudo('mkdir -p /var/log/workers && chown %s:%s %s' % (user, group, LOG_DIR))