package trycb.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

@Service
public class FlightPath {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlightPath.class);

    /**
     * Find all flight paths.
     */
    public static List<Map<String, Object>> findAll(final Bucket bucket, String from, String to, Calendar leave) {
        Statement query = select(x("faa").as("fromAirport"))
            .from(i(bucket.name()))
            .where(x("airportname").eq(s(from)))
            .union()
            .select(x("faa").as("toAirport"))
            .from(i(bucket.name()))
            .where(x("airportname").eq(s(to)));

        logQuery(query.toString());
        N1qlQueryResult result = bucket.query(N1qlQuery.simple(query));

        if (!result.finalSuccess()) {
            LOGGER.warn("Query returned with errors: " + result.errors());
            throw new DataRetrievalFailureException("Query error: " + result.errors());
        }

        String fromAirport = null;
        String toAirport = null;
        for (N1qlQueryRow row : result) {
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

        N1qlQueryResult otherResult = bucket.query(joinQuery);
        return extractResultOrThrow(otherResult);
    }

    /**
     * Extract a N1Ql result or throw if there is an issue.
     */
    private static List<Map<String, Object>> extractResultOrThrow(N1qlQueryResult result) {
        if (!result.finalSuccess()) {
            LOGGER.warn("Query returned with errors: " + result.errors());
            throw new DataRetrievalFailureException("Query error: " + result.errors());
        }

        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        for (N1qlQueryRow row : result) {
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
