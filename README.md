# workers
## description
Workers framework for multitenant processing of "work messages" by "processors".
This is extract from internal production code.

* it has "multitenant" support(each message will be marked with tenant id), services can depend on current tenant id in some way(e.g. connection to tenant db)
* besides java processors only python processors are supported(java process gets message from rabbitmq and forwards it to python subprocess)
* it supports dependency between processing(when few tasks should be "joined" at the end(like barrier task or "join" task)

## quick start
* git clone https://github.com/rapen/workers.git
* cd workers
* vagrant up
* gradlew build


## provisioning
For local development vagrant can be used(it uses 2 docker containers for rabbitmq & postgres).

## deployment

## TODO 
- deployment not tested and not specified
- documentation & examples
- refactoring & cleanup from postgres(currently it's dependent on postgres for saving internal state when tasks need to "join"), we can use redis as well for this
