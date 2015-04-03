# try-cb-java
A sample application and dataset for getting started with Couchbase 4.0. The application runs a single page UI for 
demonstrating query capabilities. The application uses Couchbase Server + Spring Boot + Angular and boostrap. The 
application is a flight planner that allows the user to search for and select a flight route (including return 
flight) based on airports and dates. Airport selection is done dynamically using an angular typeahead bound to cb 
server query. Date selection uses date time pickers and then searches for applicable air flight routes from a 
previously populated database.

## How to Run it

Either run from the IDE or just go into the directory and run:

```
mvn spring-boot:run
```

More info will be added here soon.