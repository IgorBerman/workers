# workers
Workers framework for multitenant processing of "work messages" by "processors".
This is extract from internal production code.
For local development vagrant can be used(it uses 2 docker containers for rabbitmq & postgres).
Still work to do in few directions:
1. deployment not tested and not specified
2. documentation
3. refactoring & cleanup from postgres(currently it's dependent on postgres for saving internal state when tasks need to "join")
