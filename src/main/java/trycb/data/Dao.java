/**
 * Copyright (C) 2015 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package trycb.data;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryRow;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.Sort;
import com.couchbase.client.java.query.dsl.path.AsPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

@Component
public class Dao {

    private static final Logger LOGGER = LoggerFactory.getLogger(Dao.class);

    private Dao() {}

    /**
     * Find all airports.
     */
    public static List<Map<String, Object>> findAllAirports(final Bucket bucket, final String params) {
        Statement query;

        AsPath prefix = select("airportname").from(i(bucket.name()));
        if (params.length() == 3) {
            query = prefix.where(x("faa").eq(s(params.toUpperCase())));
        } else if (params.length() == 4 && (params.equals(params.toUpperCase()) || params.equals(params.toLowerCase()))) {
            query = prefix.where(x("icao").eq(s(params.toUpperCase())));
        } else {
            query = prefix.where(i("airportname").like(s(params + "%")));
        }

        logQuery(query.toString());
        QueryResult result = bucket.query(Query.simple(query));
        return extractResultOrThrow(result);
    }

    /**
     * Find all flight paths.
     */
    public static List<Map<String, Object>> findAllFlightPaths(final Bucket bucket, String from, String to, Calendar leave) {
        Statement query = select(x("faa").as("fromAirport"))
            .from(i(bucket.name()))
            .where(x("airportname").eq(s(from)))
            .union()
            .select(x("faa").as("toAirport"))
            .from(i(bucket.name()))
            .where(x("airportname").eq(s(to)));

        logQuery(query.toString());
        QueryResult result = bucket.query(Query.simple(query));

        if (!result.finalSuccess()) {
            LOGGER.warn("Query returned with errors: " + result.errors());
            throw new DataRetrievalFailureException("Query error: " + result.errors());
        }

        String fromAirport = null;
        String toAirport = null;
        for (QueryRow row : result) {
            if (row.value().containsKey("fromAirport")) {
                fromAirport = row.value().getString("fromAirport");
            }
            if (row.value().containsKey("toAirport")) {
                toAirport = row.value().getString("toAirport");
            }
        }

        Statement joinQuery = select("a.name", "s.flight", "s.utc", "r.sourceairport", "r.destinationairport", "r.equipment")
                .from(i(bucket.name()).as("r"))
                .unnest("r.schedule AS s")
                .join(i(bucket.name()).as("a") + " ON KEYS r.airlineid")
                .where(x("r.sourceairport").eq(s(fromAirport)).and(x("r.destinationairport").eq(s(toAirport))).and(x("s.day").eq(leave.get(Calendar.DAY_OF_WEEK))))
                .orderBy(Sort.asc("a.name"));
        logQuery(joinQuery.toString());

        QueryResult otherResult = bucket.query(joinQuery);
        return extractResultOrThrow(otherResult);
    }

    /**
     * Try to log the given user in.
     */
    public static ResponseEntity<String> login(final Bucket bucket, final String username, final String password) {
        JsonDocument doc = bucket.get("user::" + username);

        JsonObject responseContent;
        if(BCrypt.checkpw(password, doc.content().getString("password"))) {
            responseContent = JsonObject.create().put("success", true).put("data", doc.content());
        } else {
            responseContent = JsonObject.empty().put("success", false).put("failure", "Bad Username or Password");
        }
        return new ResponseEntity<String>(responseContent.toString(), HttpStatus.OK);
    }

    /**
     * Create a user.
     */
    public static ResponseEntity<String> createLogin(final Bucket bucket, final String username, final String password) {
        JsonObject data = JsonObject.create()
            .put("type", "user")
            .put("name", username)
            .put("password", BCrypt.hashpw(password, BCrypt.gensalt()));
        JsonDocument doc = JsonDocument.create("user::" + username, data);

        try {
            bucket.insert(doc);
            JsonObject responseData = JsonObject.create()
                .put("success", true)
                .put("data", data);
            return new ResponseEntity<String>(responseData.toString(), HttpStatus.OK);
        } catch (Exception e) {
            JsonObject responseData = JsonObject.empty()
                .put("success", false)
                .put("failure", "There was an error creating account")
                .put("exception", e.getMessage());
            return new ResponseEntity<String>(responseData.toString(), HttpStatus.OK);
        }
    }

    /**
     * Register a flight (or flights) for the given user.
     */
    public static ResponseEntity<String> registerFlightForUser(final Bucket bucket, final String username, final JsonArray newFlights) {
        JsonDocument userData = bucket.get("user::" + username);
        if (userData == null) {
            throw new IllegalStateException("A user needs to be created first.");
        }

        JsonArray allBookedFlights = userData.content().getArray("flights");
        if(allBookedFlights == null) {
            allBookedFlights = JsonArray.create();
        }

        for (Object newFlight : newFlights) {
            JsonObject t = ((JsonObject) newFlight).getObject("_data");
            JsonObject flightJson = JsonObject.empty()
                .put("name", t.get("name"))
                .put("flight", t.get("flight"))
                .put("date", t.get("date"))
                .put("sourceairport", t.get("sourceairport"))
                .put("destinationairport", t.get("destinationairport"))
                .put("bookedon", "");
            allBookedFlights.add(flightJson);
        }

        userData.content().put("flights", allBookedFlights);
        JsonDocument response = bucket.upsert(userData);
        JsonObject responseData = JsonObject.create()
            .put("added", response.content().getArray("flights").size());
        return new ResponseEntity<String>(responseData.toString(), HttpStatus.OK);
    }

    /**
     * Show all booked flights for the given user.
     */
    public static ResponseEntity<String> getFlightsForUser(final Bucket bucket, final String username) {
        JsonDocument doc = bucket.get("user::" + username);
        if(doc != null) {
            return new ResponseEntity<String>(doc.content().getArray("flights").toString(), HttpStatus.OK);
        }

        return new ResponseEntity<String>("{failure: 'No flights found'}", HttpStatus.OK);
    }

    /**
     * Extract a N1Ql result or throw if there is an issue.
     */
    private static List<Map<String, Object>> extractResultOrThrow(QueryResult result) {
        if (!result.finalSuccess()) {
            LOGGER.warn("Query returned with errors: " + result.errors());
            throw new DataRetrievalFailureException("Query error: " + result.errors());
        }

        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        for (QueryRow row : result) {
            content.add(row.value().toMap());
        }
        return content;
    }

    /**
     * Helper method to log the executing query.
     */
    private static void logQuery(String query) {
        LOGGER.info("Executing Query: {}", query);
    }

}
