# Couchbase Java Travel-Sample Application
This is a sample application for getting started with Couchbase Server 4.5. The application runs a single page UI for
demonstrating SQL for Documents (N1QL) and Full Text Search (FTS) querying capabilities. It uses Couchbase Server 5.0
together with Spring Boot, Angular2 and Bootstrap.

The application is a flight planner that allows the user to search for and select a flight route (including the
return flight) based on airports and dates. Airport selection is done dynamically using an angular autocomplete box
bound to N1QL queries on the server side. After selecting a date, it then searches for applicable air flight routes from
a previously populated database. An additional page allows users to search for Hotels using less structured keywords.

![Application](app.png)

## Prerequisites
The following pieces need to be in place in order to run the application.

1. Couchbase Server 5.0 or later with the `travel-sample` bucket.
2. Java 6 or later
3. Maven 3 or later

If you want to run the application from your IDE rather than from the command line you also need your IDE set up to
work with maven-based projects. We recommend running IntelliJ IDEA, but Eclipse or Netbeans will also work.

Note that the project uses Lombok, so code generation through annotation processing must be enabled.

## Running the Application
To download the application you can clone the repository:

```
$ git clone https://github.com/couchbaselabs/try-cb-java.git
```

Now change into the directory (`$ cd try-cb-java`) and then run the following maven command.

```
mvn spring-boot:run
```

If all goes well, this will start a web server on `127.0.0.1:8080`. 

```
$ mvn spring-boot:run
[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building try-cb-java 2.0.0
[INFO] ------------------------------------------------------------------------
[INFO] 
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v1.2.5.RELEASE)

2016-09-12 14:15:36.438  INFO 64357 --- [lication.main()] trycb.Application                        : Starting Application on SimonBookPro.local with PID 64357 (/Users/edralzar/dev/couchbase/misc/try-cb-java/target/classes started by edralzar in /Users/edralzar/dev/couchbase/misc/try-cb-java)
2016-09-12 14:15:37.606  INFO 64357 --- [lication.main()] s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat initialized with port(s): 8080 (http)
2016-09-12 14:15:37.979  INFO 64357 --- [lication.main()] o.apache.catalina.core.StandardService   : Starting service Tomcat
2016-09-12 14:15:37.980  INFO 64357 --- [lication.main()] org.apache.catalina.core.StandardEngine  : Starting Servlet Engine: Apache Tomcat/8.0.23
2016-09-12 14:15:38.062  INFO 64357 --- [ost-startStop-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2016-09-12 14:15:38.063  INFO 64357 --- [ost-startStop-1] o.s.web.context.ContextLoader            : Root WebApplicationContext: initialization completed in 1594 ms
2016-09-12 14:15:38.870  INFO 64357 --- [lication.main()] com.couchbase.client.core.CouchbaseCore  : CouchbaseEnvironment: {sslEnabled=false, sslKeystoreFile='null', sslKeystorePassword=false, sslKeystore=null, bootstrapHttpEnabled=true, bootstrapCarrierEnabled=true, bootstrapHttpDirectPort=8091, bootstrapHttpSslPort=18091, bootstrapCarrierDirectPort=11210, bootstrapCarrierSslPort=11207, ioPoolSize=8, computationPoolSize=8, responseBufferSize=16384, requestBufferSize=16384, kvServiceEndpoints=1, viewServiceEndpoints=1, queryServiceEndpoints=1, searchServiceEndpoints=1, ioPool=NioEventLoopGroup, coreScheduler=CoreScheduler, eventBus=DefaultEventBus, packageNameAndVersion=couchbase-java-client/2.3.3 (git: 2.3.3, core: 1.3.3), dcpEnabled=false, retryStrategy=BestEffort, maxRequestLifetime=75000, retryDelay=ExponentialDelay{growBy 1.0 MICROSECONDS, powers of 2; lower=100, upper=100000}, reconnectDelay=ExponentialDelay{growBy 1.0 MILLISECONDS, powers of 2; lower=32, upper=4096}, observeIntervalDelay=ExponentialDelay{growBy 1.0 MICROSECONDS, powers of 2; lower=10, upper=100000}, keepAliveInterval=30000, autoreleaseAfter=2000, bufferPoolingEnabled=true, tcpNodelayEnabled=true, mutationTokensEnabled=false, socketConnectTimeout=1000, dcpConnectionBufferSize=20971520, dcpConnectionBufferAckThreshold=0.2, dcpConnectionName=dcp/core-io, callbacksOnIoPool=false, disconnectTimeout=25000, requestBufferWaitStrategy=com.couchbase.client.core.env.DefaultCoreEnvironment$2@6bf79f80, queryTimeout=75000, viewTimeout=75000, kvTimeout=2500, connectTimeout=5000, dnsSrvEnabled=false}
2016-09-12 14:15:39.456  INFO 64357 --- [      cb-io-1-1] com.couchbase.client.core.node.Node      : Connected to Node localhost
2016-09-12 14:15:39.822  INFO 64357 --- [-computations-1] c.c.c.core.config.ConfigurationProvider  : Opened bucket travel-sample
2016-09-12 14:15:39.895  INFO 64357 --- [-computations-5] c.c.c.core.config.ConfigurationProvider  : Opened bucket default
2016-09-12 14:15:39.905  INFO 64357 --- [lication.main()] trycb.util.StartupPreparations           : Ensuring all Indexes are created.
2016-09-12 14:15:40.017  INFO 64357 --- [lication.main()] trycb.util.StartupPreparations           : All indexes are already in place, nothing to build
2016-09-12 14:15:40.183  INFO 64357 --- [lication.main()] s.w.s.m.m.a.RequestMappingHandlerAdapter : Looking for @ControllerAdvice: org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext@6f7e827: startup date [Mon Sep 12 14:15:36 CEST 2016]; root of context hierarchy
2016-09-12 14:15:40.232  INFO 64357 --- [lication.main()] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/api/airports]}" onto public org.springframework.http.ResponseEntity<? extends trycb.model.IValue> trycb.web.AirportController.airports(java.lang.String)
2016-09-12 14:15:40.233  INFO 64357 --- [lication.main()] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/api/flightPaths/{from}/{to}]}" onto public org.springframework.http.ResponseEntity<? extends trycb.model.IValue> trycb.web.FlightPathController.all(java.lang.String,java.lang.String,java.lang.String)
2016-09-12 14:15:40.234  INFO 64357 --- [lication.main()] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/api/hotel/],methods=[GET],produces=[application/json]}" onto public org.springframework.http.ResponseEntity<? extends trycb.model.IValue> trycb.web.HotelController.findAllHotels()
2016-09-12 14:15:40.234  INFO 64357 --- [lication.main()] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/api/hotel/{description}/{location}/],methods=[GET],produces=[application/json]}" onto public org.springframework.http.ResponseEntity<? extends trycb.model.IValue> trycb.web.HotelController.findHotelsByDescriptionAndLocation(java.lang.String,java.lang.String)
2016-09-12 14:15:40.234  INFO 64357 --- [lication.main()] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/api/hotel/{description}/],methods=[GET],produces=[application/json]}" onto public org.springframework.http.ResponseEntity<? extends trycb.model.IValue> trycb.web.HotelController.findHotelsByDescription(java.lang.String)
2016-09-12 14:15:40.235  INFO 64357 --- [lication.main()] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/api/user/{username}/flights],methods=[POST]}" onto public org.springframework.http.ResponseEntity<? extends trycb.model.IValue> trycb.web.UserController.book(java.lang.String,java.lang.String,java.lang.String)
2016-09-12 14:15:40.235  INFO 64357 --- [lication.main()] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/api/user/{username}/flights],methods=[GET]}" onto public java.lang.Object trycb.web.UserController.booked(java.lang.String,java.lang.String)
2016-09-12 14:15:40.235  INFO 64357 --- [lication.main()] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/api/user/login],methods=[POST]}" onto public org.springframework.http.ResponseEntity<? extends trycb.model.IValue> trycb.web.UserController.login(java.util.Map<java.lang.String, java.lang.String>)
2016-09-12 14:15:40.235  INFO 64357 --- [lication.main()] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/api/user/signup],methods=[POST]}" onto public org.springframework.http.ResponseEntity<? extends trycb.model.IValue> trycb.web.UserController.createLogin(java.lang.String)
2016-09-12 14:15:40.236  INFO 64357 --- [lication.main()] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/error]}" onto public org.springframework.http.ResponseEntity<java.util.Map<java.lang.String, java.lang.Object>> org.springframework.boot.autoconfigure.web.BasicErrorController.error(javax.servlet.http.HttpServletRequest)
2016-09-12 14:15:40.236  INFO 64357 --- [lication.main()] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/error],produces=[text/html]}" onto public org.springframework.web.servlet.ModelAndView org.springframework.boot.autoconfigure.web.BasicErrorController.errorHtml(javax.servlet.http.HttpServletRequest)
2016-09-12 14:15:40.398  INFO 64357 --- [lication.main()] s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat started on port(s): 8080 (http)
2016-09-12 14:15:40.400  INFO 64357 --- [lication.main()] trycb.Application                        : Started Application in 4.172 seconds (JVM running for 16.45)
```

Note that when you run the application for the first time, it will make sure that all indexes are created for best
performance, so it might take a bit longer. You can follow the output on the command line.

You should then be able to browse the UI, search for US airports and get flight route information. If you are unsure for
what to search for, try from `SFO` to `LAX` and just use the 1st until the 5th of the current month. While doing so, you
can check the command line which prints all the executed N1QL queries:

```
2015-04-07 13:30:20.866  INFO 64284 --- [nio-8080-exec-7] trycb.Database: Executing Query: SELECT airportname FROM `travel-sample` WHERE faa = "SFO"
2015-04-07 13:30:23.219  INFO 64284 --- [nio-8080-exec-8] trycb.Database: Executing Query: SELECT airportname FROM `travel-sample` WHERE faa = "LAX"
2015-04-07 13:30:29.154  INFO 64284 --- [io-8080-exec-10] trycb.Database: Executing Query: SELECT faa AS fromAirport FROM `travel-sample` WHERE airportname = "Los Angeles Intl" UNION SELECT faa AS toAirport FROM `travel-sample` WHERE airportname = "San Francisco Intl"
2015-04-07 13:30:29.154  INFO 64284 --- [nio-8080-exec-9] trycb.Database: Executing Query: SELECT faa AS fromAirport FROM `travel-sample` WHERE airportname = "San Francisco Intl" UNION SELECT faa AS toAirport FROM `travel-sample` WHERE airportname = "Los Angeles Intl"
2015-04-07 13:30:29.222  INFO 64284 --- [io-8080-exec-10] trycb.Database: Executing Query: SELECT a.name, s.flight, s.utc, r.sourceairport, r.destinationairport, r.equipment FROM `travel-sample` r UNNEST r.schedule s JOIN `travel-sample` a ON KEYS r.airlineid WHERE r.sourceairport='LAX' AND r.destinationairport='SFO' AND s.day=5 ORDER BY a.name
2015-04-07 13:30:29.289  INFO 64284 --- [nio-8080-exec-9] trycb.Database: Executing Query: SELECT a.name, s.flight, s.utc, r.sourceairport, r.destinationairport, r.equipment FROM `travel-sample` r UNNEST r.schedule s JOIN `travel-sample` a ON KEYS r.airlineid WHERE r.sourceairport='SFO' AND r.destinationairport='LAX' AND s.day=1 ORDER BY a.name
```

## Custom Options
You can also conveniently change those options through the command line at bootstrap:

```
$ mvn spring-boot:run -Dstorage.host=127.0.0.1 -Dstorage.bucket=travel-sample -Dstorage.password=password -Dstorage.username=Administrator
```