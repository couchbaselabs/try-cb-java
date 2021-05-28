package trycb.service;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.couchbase.client.core.error.QueryException;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

import trycb.model.Result;

@Service
public class FlightPath {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlightPath.class);

    /**
     * Find all flight paths.
     */
    public static Result<List<Map<String, Object>>> findAll(final Cluster cluster, final String bucket, String from,
            String to, Calendar leave) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT faa as fromAirport ");
        builder.append("FROM `").append(bucket).append("`.inventory.airport ");
        builder.append("WHERE airportname = $from ");
        builder.append("UNION ");
        builder.append("SELECT faa as toAirport ");
        builder.append("FROM `").append(bucket).append("`.inventory.airport ");
        builder.append("WHERE airportname = $to");
        String query = builder.toString();

        logQuery(query);
        QueryResult result = null;
        try {
            result = cluster.query(query, QueryOptions.queryOptions().raw("$from", from).raw("$to", to));
        } catch (QueryException e) {
            LOGGER.warn("Query failed with exception: " + e);
            throw new DataRetrievalFailureException("Query error: " + result);
        }

        List<JsonObject> rows = result.rowsAsObject();
        String fromAirport = null;
        String toAirport = null;
        for (JsonObject obj : rows) {
            if (obj.containsKey("fromAirport")) {
                fromAirport = obj.getString("fromAirport");
            }
            if (obj.containsKey("toAirport")) {
                toAirport = obj.getString("toAirport");
            }
        }

        StringBuilder joinBuilder = new StringBuilder();
        joinBuilder.append("SELECT a.name, s.flight, s.utc, r.sourceairport, r.destinationairport, r.equipment ");
        joinBuilder.append("FROM `").append(bucket).append("`.inventory.route AS r ");
        joinBuilder.append("UNNEST r.schedule AS s ");
        joinBuilder.append("JOIN `").append(bucket).append("`.inventory.airline AS a ON KEYS r.airlineid ");
        joinBuilder.append("WHERE r.sourceairport = ? and r.destinationairport = ? ");
        joinBuilder.append("AND s.day = ? ");
        joinBuilder.append("ORDER BY a.name ASC");
        String joinQuery = joinBuilder.toString();

        JsonArray params = JsonArray.create();
        params.add(fromAirport);
        params.add(toAirport);
        params.add(leave.get(Calendar.DAY_OF_WEEK));

        logQuery(joinQuery);
        QueryResult otherResult = null;
        try {
            otherResult = cluster.query(joinQuery, QueryOptions.queryOptions().parameters(params));
        } catch (QueryException e) {
            LOGGER.warn("Query failed with exception: " + e);
            throw new DataRetrievalFailureException("Query error: " + otherResult);
        }

        List<JsonObject> resultRows = otherResult.rowsAsObject();
        Random rand = new Random();
        List<Map<String, Object>> data = new LinkedList<Map<String, Object>>();
        for (JsonObject row : resultRows) {
            row.put("flighttime", rand.nextInt(8000));
            row.put("price", Math.ceil(row.getDouble("flighttime") / 8 * 100) / 100);
            data.add(row.toMap());
        }

        String querytype = "N1QL query - scoped to inventory: ";
        return Result.of(data, querytype, joinQuery);
    }

    /**
     * Helper method to log the executing query.
     */
    private static void logQuery(String query) {
        LOGGER.info("Executing Query: {}", query);
    }
}
