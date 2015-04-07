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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public static List<Map<String, Object>> findAllAirports(final Bucket bucket, final String query) {
        Statement q;

        AsPath prefix = select("airportname").from(i(bucket.name()));
        if (query.length() == 3) {
            q = prefix.where(x("faa").eq(s(query.toUpperCase())));
        } else if (query.length() == 4 && (query.equals(query.toUpperCase())||query.equals(query.toLowerCase()))) {
            q = prefix.where(x("icao").eq(s(query.toUpperCase())));
        } else {
            q = prefix.where(i("airportname").like(s(query + "%")));
        }

        logQuery(q.toString());
        QueryResult result = bucket.query(Query.simple(q));

        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        if (result.finalSuccess()) {
            for (QueryRow row : result) {
                content.add(row.value().toMap());
            }
        } else {
            System.err.println(result.errors());
        }

        return content;
    }

    public static List<Map<String, Object>> findAllFlightPaths(final Bucket bucket, String from, String to, Calendar leave) {
        Statement query = select(x("faa").as("fromAirport")).from(i(bucket.name())).where(x("airportname").eq(s(from)))
            .union()
            .select(x("faa").as("toAirport")).from(i(bucket.name())).where(x("airportname").eq(s(to)));

        logQuery(query.toString());
        QueryResult result = bucket.query(Query.simple(query));

        String fromAirport = null;
        String toAirport = null;
        for (QueryRow row : result) {
            if (fromAirport == null) {
                fromAirport = row.value().getString("fromAirport");
            }
            if (toAirport == null) {
                toAirport = row.value().getString("toAirport");
            }
        }



        String otherQuery = "SELECT a.name, s.flight, s.utc, r.sourceairport, r.destinationairport, r.equipment FROM `"
            + bucket.name() + "` r UNNEST r.schedule s JOIN `" + bucket.name() + "` a ON KEYS r.airlineid WHERE r.sourceairport='"
            + fromAirport + "' AND r.destinationairport='" + toAirport + "' AND s.day=" + leave.get(Calendar.DAY_OF_MONTH) + " ORDER BY a.name";


        logQuery(otherQuery);
        QueryResult otherResult = bucket.query(Query.simple(otherQuery));

        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        if (otherResult.finalSuccess()) {
            for (QueryRow row : otherResult) {
                content.add(row.value().toMap());
            }
        } else {
            System.err.println(result.errors());
        }

        return content;
    }

    private static void logQuery(String query) {
        LOGGER.info("Executing Query: {}", query);
    }

}
