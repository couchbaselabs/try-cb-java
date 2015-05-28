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
package trycb;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryRow;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.path.AsPath;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.codec.Base64;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

public class Database {

    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    private Database() {}

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

    public static List<Map<String, Object>> findAllFlightPaths(final Bucket bucket, String from, String to, Calendar leave) {
        Statement query = select(x("faa").as("fromAirport")).from(i(bucket.name())).where(x("airportname").eq(s(from)))
            .union()
            .select(x("faa").as("toAirport")).from(i(bucket.name())).where(x("airportname").eq(s(to)));

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

        String joinQuery = "SELECT a.name, s.flight, s.utc, r.sourceairport, r.destinationairport, r.equipment FROM `"
            + bucket.name() + "` r UNNEST r.schedule s JOIN `" + bucket.name() + "` a ON KEYS r.airlineid WHERE r.sourceairport='"
            + fromAirport + "' AND r.destinationairport='" + toAirport + "' AND s.day="
            + leave.get(Calendar.DAY_OF_MONTH) + " ORDER BY a.name";

        logQuery(joinQuery);
        QueryResult otherResult = bucket.query(Query.simple(joinQuery));
        return extractResultOrThrow(otherResult);
    }

    public static ResponseEntity<String> login(final Bucket bucket, final String username, final String password) {
        JsonDocument doc = bucket.get("user::" + username);
        if(BCrypt.checkpw(password, doc.content().getString("password"))) {
            JsonObject response = JsonObject.create()
                .put("success", "sometokenhere")
                .put("data", doc.content());
            return new ResponseEntity<String>(response.toString(), HttpStatus.OK);
        }
        JsonObject responseData = JsonObject.empty()
            .put("failure", "Bad Username or Password");
        return new ResponseEntity<String>(responseData.toString(), HttpStatus.OK);
    }

    public static ResponseEntity<String> createLogin(final Bucket bucket, final String username, final String password) {
        JsonObject data = JsonObject.empty()
            .put("_type", "User")
            .put("_id", "")
            .put("token", Base64.encode(("{user: " + username + "}").getBytes()).toString())
            .put("name", username)
            .put("password", BCrypt.hashpw(password, BCrypt.gensalt()));
        JsonDocument doc = JsonDocument.create("user::" + username, data);
        try {
            JsonDocument response = bucket.insert(doc);
            JsonObject responseData = JsonObject.create()
                .put("success", "sometokenhere")
                .put("data", data);
            return new ResponseEntity<String>(responseData.toString(), HttpStatus.OK);
        } catch (Exception e) {
            JsonObject responseData = JsonObject.empty()
                .put("failure", "There was an error createing account")
                .put("exception", e.getMessage());
            return new ResponseEntity<String>(responseData.toString(), HttpStatus.OK);
        }
    }

    public static ResponseEntity<String> flights(final Bucket bucket, final String token, final String newFlights) {
        JsonDocument userData = bucket.get("user::" + token);
        if(userData != null) {
            JsonArray allBookedFlights = userData.content().getArray("flights");
            if(allBookedFlights == null) {
                allBookedFlights = JsonArray.create();
            }
            JsonArray newFlightsJson = JsonArray.fromJson(newFlights);
            JsonObject curFlightJsonObject = null;
            for(Object temp : newFlightsJson.toList()) {
                Map<String, Object> t = (Map<String, Object>) ((Map<String, Object>) temp).get("_data");
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
        return null;
    }

    public static ResponseEntity<String> getFlights(final Bucket bucket, final String token) {
        JsonDocument doc = bucket.get("user::" + token);
        if(doc != null) {
            return new ResponseEntity<String>(doc.content().getArray("flights").toString(), HttpStatus.OK);
        }
        return new ResponseEntity<String>("{failure: 'No flights found'}", HttpStatus.OK);
    }

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

    private static void logQuery(String query) {
        LOGGER.info("Executing Query: {}", query);
    }

}
