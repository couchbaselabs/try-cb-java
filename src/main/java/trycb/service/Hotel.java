package trycb.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.couchbase.client.core.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.SearchOptions;
import com.couchbase.client.java.search.queries.ConjunctionQuery;
import com.couchbase.client.java.search.result.SearchRow;
import com.couchbase.client.java.search.result.SearchResult;
import com.couchbase.client.java.json.JacksonTransformers;
import com.couchbase.client.java.kv.LookupInResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import trycb.model.Result;

import static com.couchbase.client.java.kv.LookupInSpec.*;

@Service
public class Hotel {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hotel.class);

    private Bucket bucket;

    @Autowired
    public Hotel(Bucket bucket) {
        this.bucket = bucket;
    }

    /**
     * Search for a hotel in a particular location.
     */
    public Result<List<Map<String, Object>>> findHotels(final Cluster cluster, final String location, final String description) {
        ConjunctionQuery fts = SearchQuery.conjuncts(SearchQuery.term("hotel").field("type"));

        if (location != null && !location.isEmpty() && !"*".equals(location)) {
            fts.and(SearchQuery.disjuncts(
                        SearchQuery.matchPhrase(location).field("country"),
                        SearchQuery.matchPhrase(location).field("city"),
                        SearchQuery.matchPhrase(location).field("state"),
                        SearchQuery.matchPhrase(location).field("address")
                ));
        }

        if (description != null && !description.isEmpty() && !"*".equals(description)) {
            fts.and(
                SearchQuery.disjuncts(
                        SearchQuery.matchPhrase(description).field("description"),
                        SearchQuery.matchPhrase(description).field("name")
                ));
        }

        logQuery(fts.export().toString());
        SearchOptions opts = SearchOptions.searchOptions().limit(100);
        SearchResult result = cluster.searchQuery("hotels", fts, opts);

        //prepare the context to send to the app
        String ftsContext;
        try {
            ftsContext = JacksonTransformers.MAPPER.writerWithDefaultPrettyPrinter().
                    writeValueAsString(fts.export());
        } catch (JsonProcessingException e) {
            ftsContext = fts.export().toString();
        }
        String subdocContext = "        Optional<LookupInResult> lookup = bucket.defaultCollection().lookupIn(row.id(),\n" +
                "                Arrays.asList(get(\"country\"), get(\"city\"), get(\"state\"), get(\"address\"),\n" +
                "                        get(\"name\"), get(\"description\")));";

        return Result.of(extractResultOrThrow(result), ftsContext, subdocContext);
    }

    /**
     * Search for an hotel.
     */
    public Result<List<Map<String, Object>>> findHotels(final Cluster cluster, final String description) {
        return findHotels(cluster, "*", description);
    }

    /**
     * Find all hotels.
     */
    public Result<List<Map<String, Object>>> findAllHotels(final Cluster cluster) {
        return findHotels(cluster, "*", "*");
    }

    /**
     * Extract a FTS result or throw if there is an issue.
     */
    private List<Map<String, Object>> extractResultOrThrow(SearchResult result) {
        if (result.metaData().metrics().errorPartitionCount() > 0) {
            LOGGER.warn("Query returned with errors: " + result.metaData().errors());
            throw new DataRetrievalFailureException("Query error: " + result.metaData().errors());
        }

        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        for (SearchRow row : result.rows()) {

            LookupInResult res;
            try {
                res = bucket.defaultCollection().lookupIn(row.id(),
                    Arrays.asList(get("country"), get("city"), get("state"), get("address"),
                        get("name"), get("description")));
            } catch (DocumentNotFoundException ex) {
                continue;
            }

            Map<String, Object> map = new HashMap<String, Object>();

            String country = res.contentAs(0, String.class);
            String city = res.contentAs(1, String.class);
            String state = res.contentAs(2, String.class);
            String address = res.contentAs(3, String.class);

            StringBuilder fullAddr = new StringBuilder();
            if (address != null)
                fullAddr.append(address).append(", ");
            if (city != null)
                fullAddr.append(city).append(", ");
            if (state != null)
                fullAddr.append(state).append(", ");
            if (country != null)
                fullAddr.append(country);

            if (fullAddr.length() > 2 && fullAddr.charAt(fullAddr.length() - 2) == ',')
                fullAddr.delete(fullAddr.length() - 2, fullAddr.length() - 1);

            map.put("name", res.contentAs(4, String.class));
            map.put("description", res.contentAs(5, String.class));
            map.put("address", fullAddr.toString());

            content.add(map);
        }
        return content;
    }

    /**
     * Helper method to log the executing query.
     */
    private static void logQuery(String query) {
        LOGGER.info("Executing FTS Query: {}", query);
    }

}
