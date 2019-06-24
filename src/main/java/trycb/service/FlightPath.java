package trycb.service;

import com.couchbase.client.core.error.QueryServiceException;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import trycb.model.Result;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class FlightPath {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlightPath.class);

    /**
     * Find all flight paths.
     */
    public static Result<List<Map<String, Object>>> findAll(final Cluster cluster, final String bucket, String from, String to, Calendar leave) {
        StringBuilder builder = new StringBuilder();
        builder.append("select faa as fromAirport ");
        builder.append("from `").append(bucket).append("` ");
        builder.append("where airportname = '").append(from).append("' ");
        builder.append("union ");
        builder.append("select faa as toAirport ");
        builder.append("from `").append(bucket).append("` ");
        builder.append("where airportname = '").append(to).append("' ");
        String query = builder.toString();

        logQuery(query);
        QueryResult result = null;
        try {
            result = cluster.query(query);
        } catch (QueryServiceException e) {
            LOGGER.warn("Query failed with exception: " + e);
            throw new DataRetrievalFailureException("Query error: " + result);
        }

        List<JsonObject> rows = result.allRowsAsObject();
        String fromAirport = null;
        String toAirport = null;
        for (JsonObject obj: rows) {
            if (obj.containsKey("fromAirport")) {
                fromAirport = obj.getString("fromAirport");
            }
            if (obj.containsKey("toAirport")) {
                toAirport = obj.getString("toAirport");
            }
        }

        StringBuilder joinBuilder = new StringBuilder();
        joinBuilder.append("select a.name, s.flight, s.utc, r.sourceairport, r.destinationairport, r.equipment ");
        joinBuilder.append("from `").append(bucket).append("` as r ");
        joinBuilder.append("unnest r.schedule as s ");
        joinBuilder.append("join `").append(bucket).append("` as a on keys r.airlineid ");
        joinBuilder.append("where r.sourceairport = '"). append(fromAirport).append("' and r.destinationairport = '").append(toAirport).append("' ");
        joinBuilder.append("and s.day = ").append(leave.get(Calendar.DAY_OF_WEEK)).append(" ");
        joinBuilder.append("order by a.name asc");
        String joinQuery = joinBuilder.toString();

        logQuery(joinQuery);
        QueryResult otherResult = null;
        try {
            otherResult = cluster.query(joinQuery);
        } catch (QueryServiceException e) {
            LOGGER.warn("Query failed with exception: " + e);
            throw new DataRetrievalFailureException("Query error: " + otherResult);
        }

        List<JsonObject> resultRows = otherResult.allRowsAsObject();
        Random rand = new Random();
        List<Map<String, Object>> data = new LinkedList<Map<String, Object>>();
        for (JsonObject row : resultRows) {
            row.put("price", rand.nextInt(2000));
            data.add(row.toMap());
        }

        return Result.of(data, joinQuery);
    }

    /**
     * Helper method to log the executing query.
     */
    private static void logQuery(String query) {
        LOGGER.info("Executing Query: {}", query);
    }
}
