# workers
## description
Workers framework for multitenant processing of "work messages" by "processors".
This is extract from internal production code.

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
- documentation
- refactoring & cleanup from postgres(currently it's dependent on postgres for saving internal state when tasks need to "join")
