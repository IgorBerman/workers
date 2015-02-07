#!/bin/bash

VM_ARGS="-Duser.timezone=GMT"
VM_ARGS="$VM_ARGS -Dfile.encoding=UTF-8"
VM_ARGS="$VM_ARGS -Djava.awt.headless=true"
VM_ARGS="$VM_ARGS -Dcom.worker.logs=/var/log/workers"
VM_ARGS="$VM_ARGS -Dpid.dir=/tmp/var/run/workers"
VM_ARGS="$VM_ARGS -Dcode.dir=/u/apps/worker-framework/current/worker-framework"
VM_ARGS="$VM_ARGS -Dpython.logs.cleanup=false" 	
VM_ARGS="$VM_ARGS -Dcom.greylog.host=eu-prod-graylog.com"

DEBUG_ARGS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8888,suspend=y"
#VM_ARGS="$VM_ARGS $DEBUG_ARGS"

if [ "$1" = "--debug" ]; then
        VM_ARGS="$VM_ARGS $DEBUG_ARGS"
        shift #removing it
fi

#since * is works only on 1 sublevel, we manually construct classpath from jars that are under .javaworkerclasspath dir
CLASSPATH=`find -L /home/.javaworkerclasspath -name *.jar | tr '\n' ':'`

#exec ensures that bash will "become" java, so supervisor will send SIGTERM to java and not to bash process while restarting java
exec java -Xmx1000m $VM_ARGS -cp "$CLASSPATH" com.worker.framework.WorkerMain
