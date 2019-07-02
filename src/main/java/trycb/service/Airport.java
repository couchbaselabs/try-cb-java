package trycb.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import trycb.model.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class Airport {

    private static final Logger LOGGER = LoggerFactory.getLogger(Airport.class);


    /**
     * Find all airports.
     */
    public static Result<List<Map<String, Object>>> findAll(final Cluster cluster, final Bucket bucket, final String params) {
        //Statement query;
        String query;

        String prefix = "SELECT airportname FROM `" + bucket.name() + "` ";
        if (params.length() == 3) {
            query = prefix + " WHERE faa = \"" + params.toUpperCase() + "\"";
        } else if (params.length() == 4 && (params.equals(params.toUpperCase()) || params.equals(params.toLowerCase()))) {
            query = prefix + " WHERE icao = \"" + params.toUpperCase() + "\"";
        } else {
            query = prefix + " WHERE faa LIKE \"" + params + "%\"";
        }

        logQuery(query);
        QueryResult result = cluster.query(query);
        List<Map<String, Object>> data = extractResultOrThrow(result);
        return Result.of(data, query);
    }

    /**
     * Extract a N1Ql result or throw if there is an issue.
     */
    private static List<Map<String, Object>> extractResultOrThrow(QueryResult result) {
        if (!result.meta().status().equals(QueryStatus.SUCCESS)) {
            LOGGER.warn("Query returned with errors: " + result.meta().status());
            throw new DataRetrievalFailureException("Query error: " + result.meta().status());
        }

        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        for (JsonObject row : result.allRowsAsObject()) {
            content.add(row.toMap());
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
