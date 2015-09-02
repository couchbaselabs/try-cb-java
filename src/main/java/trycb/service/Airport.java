package trycb.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.path.AsPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

@Service
public class Airport {

    private static final Logger LOGGER = LoggerFactory.getLogger(Airport.class);


    /**
     * Find all airports.
     */
    public static List<Map<String, Object>> findAll(final Bucket bucket, final String params) {
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
        N1qlQueryResult result = bucket.query(N1qlQuery.simple(query));
        return extractResultOrThrow(result);
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
