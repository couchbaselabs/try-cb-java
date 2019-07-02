package trycb.service;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.QueryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import trycb.model.Result;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class FlightPath {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlightPath.class);

    /**
     * Find all flight paths.
     */
    public static Result<List<Map<String, Object>>> findAll(final Cluster cluster, Bucket bucket, String from, String to, Calendar leave) {
        String query = "SELECT faa AS fromAirport FROM `" + bucket.name() + "` WHERE airportname = \"" + from + "\" UNION " +
                "SELECT faa AS toAirport FROM `" + bucket.name() + "` WHERE airportname = \"" + to + "\"";
        logQuery(query);
        QueryResult result = cluster.query(query);
        List<JsonObject> results = result.allRowsAsObject();

        if (!result.meta().status().equals(QueryStatus.SUCCESS)){
            LOGGER.warn("Query returned with errors: " + result.meta().status());
            throw new DataRetrievalFailureException("Query error: " + result.meta().status());
        }

        String fromAirport = null;
        String toAirport = null;

        for (JsonObject row : results) {
            if (row.containsKey("fromAirport")) {
                fromAirport = row.getString("fromAirport");
            }
            if (row.containsKey("toAirport")) {
                toAirport = row.getString("toAirport");
            }
        }

        String joinQuery = "SELECT a.name, s.flight, s.utc, r.sourceairport, r.destinationairport, r.equipment FROM `" +
                bucket.name() + "` AS r UNNEST r.schedule AS s JOIN `" + bucket.name() + "` AS a ON KEYS r.airlineid WHERE " +
                "r.sourceairport = \"" + fromAirport + "\" AND r.destinationairport = \"" + toAirport + "\" AND s.day = " +
                leave.get(Calendar.DAY_OF_WEEK) + " ORDER BY a.name";
        logQuery(joinQuery);

        QueryResult otherResult = cluster.query(joinQuery);

        List<Map<String, Object>> finalResult = extractResultOrThrow(otherResult);
        return Result.of(finalResult, query, joinQuery);
    }

    /**
     * Extract a N1Ql result or throw if there is an issue.
     */
    private static List<Map<String, Object>> extractResultOrThrow(QueryResult result) {
        if (!result.meta().status().equals(QueryStatus.SUCCESS)) {
            LOGGER.warn("Query returned with errors: " + result.meta().status());
            throw new DataRetrievalFailureException("Query error: " + result.meta().status());
        }

        Random rand = new Random();

        List<JsonObject> results = result.allRowsAsObject();
        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        for (JsonObject row : results) {
            content.add(row
                    .put("price", rand.nextInt(2000))
                    .toMap());
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
