# Couchbase Java Travel-Sample Application

This is a sample application for getting started with [Couchbase Server] and the [Java SDK].
The application runs a single page web UI for demonstrating SQL for Documents (N1QL), Sub-document requests and Full Text Search (FTS) querying capabilities.
It uses Couchbase Server together with the [Spring Boot] web framework for [Java], [Swagger] for API documentation, [Vue] and [Bootstrap].

The application is a flight planner that allows the user to search for and select a flight route (including the return flight) based on airports and dates.
Airport selection is done dynamically using an autocomplete box bound to N1QL queries on the server side. After selecting a date, it then searches
for applicable air flight routes from a previously populated database. An additional page allows users to search for Hotels using less structured keywords.

![Application](app.png)


## Prerequisites

To download the application you can either download [the archive](https://github.com/couchbaselabs/try-cb-java/archive/master.zip) or clone the repository:

    git clone https://github.com/couchbaselabs/try-cb-java.git

<!-- If you want to run the application from your IDE rather than from the command line you also need your IDE set up to
work with maven-based projects. We recommend running IntelliJ IDEA, but Eclipse or Netbeans will also work. -->

We recommend running the application with Docker, which starts up all components for you,
but you can also run it in a Mix-and-Match style, which we'll decribe below.

## Running the application with Docker

You will need [Docker](https://docs.docker.com/get-docker/) installed on your machine in order to run this application as we have defined a [_Dockerfile_](Dockerfile) and a [_docker-compose.yml_](docker-compose.yml) to run Couchbase Server 7.0.0, the front-end [Vue app](https://github.com/couchbaselabs/try-cb-frontend-v2.git) and the Java REST API.

To launch the full application, simply run this command from a terminal:

    docker-compose up

> **_NOTE:_** You may need more than the default RAM to run the images.
We have tested the travel-sample apps with 4.5 GB RAM configured in Docker's Preferences... -> Resources -> Memory.
When you run the application for the first time, it will pull/build the relevant docker images, so it might take a bit of time.

This will start the Java backend, Couchbase Server 7.0.0 and the Vue frontend app.

```
❯ docker-compose up
...
Creating couchbase-sandbox-7.0.0      ... done
Creating try-cb-api                   ... done
Creating try-cb-fe                    ... done
Attaching to couchbase-sandbox-7.0.0, try-cb-api, try-cb-fe
couchbase-sandbox-7.0.0 | Starting Couchbase Server -- Web UI available at http://<ip>:8091
couchbase-sandbox-7.0.0 | and logs available in /opt/couchbase/var/lib/couchbase/logs
couchbase-sandbox-7.0.0 | Configuring Couchbase Server.  Please wait (~60 sec)...
try-cb-api  | wait-for-couchbase: checking http://db:8091/pools/default/buckets/travel-sample/
try-cb-api  | wait-for-couchbase: polling for '.scopes | map(.name) | contains(["inventory", "
try-cb-fe   | wait-for-it: waiting 400 seconds for backend:8080
try-cb-api  | wait-for-couchbase: ...
couchbase-sandbox-7.0.0 | Configuration completed!
couchbase-sandbox-7.0.0 | Couchbase Admin UI: http://localhost:8091 
couchbase-sandbox-7.0.0 | Login credentials: Administrator / password
try-cb-api  | wait-for-couchbase: checking http://db:8094/api/cfg
try-cb-api  | wait-for-couchbase: polling for '.status == "ok"'
try-cb-api  | wait-for-couchbase: checking http://db:8094/api/index/hotels-index
try-cb-api  | wait-for-couchbase: polling for '.status == "ok"'
try-cb-api  | wait-for-couchbase: Failure
try-cb-api  | wait-for-couchbase: Creating hotels-index...
try-cb-api  | wait-for-couchbase: checking http://db:8094/api/index/hotels-index/count
try-cb-api  | wait-for-couchbase: polling for '.count >= 917'
try-cb-api  | wait-for-couchbase: ...
try-cb-api  | wait-for-couchbase: ...
try-cb-api  | wait-for-couchbase: checking http://db:9102/api/v1/stats
try-cb-api  | wait-for-couchbase: polling for '.indexer.indexer_state == "Active"'
try-cb-api  | wait-for-couchbase: polling for '. | keys | contains(["travel-sample:def_airport
try-cb-api  | wait-for-couchbase: polling for '. | del(.indexer) | del(.["travel-sample:def_na
try-cb-api  | wait-for-couchbase: value is currently:
try-cb-api  | false
try-cb-api  | wait-for-couchbase: ...
try-cb-api  | wait-for-couchbase: polling for '. | del(.indexer) | map(.num_pending_requests =
try-cb-api  | 
try-cb-api  |   .   ____          _            __ _ _
try-cb-api  |  /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
try-cb-api  | ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
try-cb-api  |  \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
try-cb-api  |   '  |____| .__|_| |_|_| |_\__, | / / / /
try-cb-api  |  =========|_|==============|___/=/_/_/_/
try-cb-api  |  :: Spring Boot ::                (v2.5.0)
try-cb-api  | 
try-cb-api  | 2021-06-04 14:47:12.896  INFO 1 --- [           main] trycb.Application                        : Starting Application v2.3.0 using Java 1.8.0_292 on e7a5966cfaad with PID 1 (/app/target/try-cb-java.jar started by root in /app)
try-cb-api  | 2021-06-04 14:47:12.908  INFO 1 --- [           main] trycb.Application                        : No active profile set, falling back to default profiles: default
try-cb-api  | 2021-06-04 14:47:17.271  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
try-cb-api  | 2021-06-04 14:47:17.335  INFO 1 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
try-cb-api  | 2021-06-04 14:47:17.335  INFO 1 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.46]
try-cb-api  | 2021-06-04 14:47:17.531  INFO 1 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
try-cb-api  | 2021-06-04 14:47:17.532  INFO 1 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 4339 ms
try-cb-api  | 2021-06-04 14:47:17.787 DEBUG 1 --- [           main] o.s.w.f.CommonsRequestLoggingFilter      : Filter 'logFilter' configured for use
try-cb-api  | 2021-06-04 14:47:19.460  INFO 1 --- [      cb-events] com.couchbase.core                       : [com.couchbase.core][DnsSrvLookupFailedEvent][75ms] DNS SRV lookup failed (name not found), trying to bootstrap from given hostname directly.
try-cb-api  | 2021-06-04 14:47:22.039  INFO 1 --- [      cb-events] com.couchbase.core                       : [com.couchbase.core][BucketOpenedEvent][1184ms] Opened bucket "travel-sample" {"coreId":"0x8503f8fb00000001"}
try-cb-api  | 2021-06-04 14:47:23.953  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
try-cb-api  | 2021-06-04 14:47:24.012  INFO 1 --- [           main] trycb.Application                        : Started Application in 12.758 seconds (JVM running for 14.829)
try-cb-api  | 2021-06-04 14:47:24.019  INFO 1 --- [           main] o.s.b.a.ApplicationAvailabilityBean      : Application availability state LivenessState changed to CORRECT
try-cb-api  | 2021-06-04 14:47:24.025  INFO 1 --- [           main] o.s.b.a.ApplicationAvailabilityBean      : Application availability state ReadinessState changed to ACCEPTING_TRAFFIC
try-cb-fe   | wait-for-it: backend:8080 is available after 88 seconds
try-cb-fe   | 
try-cb-fe   | > try-cb-frontend-v2@0.1.0 serve
try-cb-fe   | > vue-cli-service serve --port 8081
try-cb-fe   | 
try-cb-fe   |  INFO  Starting development server...
try-cb-fe   |  DONE  Compiled successfully in 7785ms2:47:36 PM
try-cb-fe   | 
try-cb-fe   | 
try-cb-fe   |   App running at:
try-cb-fe   |   - Local:   http://localhost:8081/ 
try-cb-fe   | 
try-cb-fe   |   It seems you are running Vue CLI inside a container.
try-cb-fe   |   Access the dev server via http://localhost:<your container's external mapped port>/
try-cb-fe   | 
try-cb-fe   |   Note that the development build is not optimized.
try-cb-fe   |   To create a production build, run npm run build.
try-cb-fe   | 
```

You should then be able to browse the UI, search for US airports and get flight
route information.

To end the application press <kbd>Control</kbd>+<kbd>C</kbd> in the terminal
and wait for docker-compose to gracefully stop your containers.

## Mix and match services

Instead of running all services, you can start any combination of `backend`,
`frontend`, `db` via docker, and take responsibility for starting the other
services yourself.

As the provided `docker-compose.yml` sets up dependencies between the services,
to make startup as smooth and automatic as possible, we also provide an
alternative `mix-and-match.yml`. We'll look at a few useful scenarios here.

### Bring your own database

If you wish to run this application against your own configuration of Couchbase
Server, you will need version 7.0.0 or later with the `travel-sample`
bucket setup.

> **_NOTE:_** If you are not using Docker to start up the API server, or the
> provided wrapper `wait-for-couchbase.sh`, you will need to create a full text
> search index on travel-sample bucket called 'hotels-index'. You can do this
> via the following command:

    curl --fail -s -u <username>:<password> -X PUT \
            http://<host>:8094/api/index/hotels-index \
            -H 'cache-control: no-cache' \
            -H 'content-type: application/json' \
            -d @fts-hotels-index.json

With a running Couchbase Server, you can pass the database details in:

    CB_HOST=10.144.211.101 CB_USER=Administrator CB_PSWD=password docker-compose -f mix-and-match.yml up backend frontend

The Docker image will run the same checks as usual, and also create the
`hotels-index` if it does not already exist.

### Running the Java API application manually

You may want to run the Java application yourself, to make rapid changes to it,
and try out the features of the Couchbase API, without having to re-build the Docker
image. You may still use Docker to run the Database and Frontend components if desired.

Please ensure that you have the following before proceeding.

* Java 8 or later (Java 11 recommended)
* Maven 3 or later

Install the dependencies:

    mvn clean install

The first time you run against a new database image, you may want to use the provided
`wait-for-couchbase.sh` wrapper to ensure that all indexes are created.
For example, using the Docker image provided:

    docker-compose -f mix-and-match.yml up db

    export CB_HOST=localhost CB_USER=Administrator CB_PSWD=password
    ./wait-for-couchbase.sh echo "Couchbase is ready!"
    
    mvn spring-boot:run -Dspring-boot.run.arguments="--storage.host=$CB_HOST storage.username=$CB_USER storage.password=$CB_PSWD"

If you already have an existing Couchbase server running and correctly configured, you might run:

    mvn spring-boot:run -Dspring-boot.run.arguments="--storage.host=localhost storage.username=Administrator storage.password=password"

Finally, if you want to see how the sample frontend Vue application works with your changes,
run it with:

    docker-compose -f mix-and-match.yml up frontend

### Running the front-end manually

To run the frontend components manually without Docker, follow the guide
[here](https://github.com/couchbaselabs/try-cb-frontend-v2)

## REST API reference, and tests.

All the travel-sample apps conform to the same interface, which means that they can all be used with the same database configuration and Vue.js frontend.

We've integrated Swagger/OpenApi version 3 documentation which can be accessed on the backend at `http://localhost:8080/apidocs` once you have started the app.

(You can also view a read-only version at https://docs.couchbase.com/java-sdk/current/hello-world/sample-application.html#)

To further ensure that every app conforms to the API, we have a [test suite][try-cb-test], which you can simply run with the command:

```
docker-compose --profile test up test
```

If you are running locally though, with a view to extending or modifying the travel-sample app, you will likely want to be able to make changes to both the code and the tests in parallel.

 * Start the backend server locally, for example using "Running the Java API application manually" above.
 * Check out the [test suite][try-cb-test] repo in a separate working directory, and run the tests manually, as per the instructions.

Check the test repo for details on how to run locally.

[Couchbase Server]: https://www.couchbase.com/
[Java SDK]: https://docs.couchbase.com/java-sdk/current/hello-world/overview.html
[Spring Boot]: https://spring.io/projects/spring-boot
[Java]: https://www.java.com/en/
[Swagger]: https://swagger.io/resources/open-api/
[Vue]: https://vuejs.org/
[Bootstrap]: https://getbootstrap.com/
[try-cb-test]: https://github.com/couchbaselabs/try-cb-test/
