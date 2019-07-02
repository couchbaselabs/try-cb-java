package trycb.service;

import java.util.*;

import com.couchbase.client.core.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.json.JacksonTransformers;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.queries.ConjunctionQuery;
import com.couchbase.client.java.search.result.SearchQueryRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import trycb.model.Result;
import com.couchbase.client.java.search.result.SearchResult;
import com.couchbase.client.java.Cluster;

@Service
public class Hotel {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hotel.class);

    private Bucket bucket;
    private Cluster cluster;

    @Autowired
    public Hotel(Cluster cluster) {
        this.cluster = cluster;
    }

    /**
     * Search for a hotel in a particular location.
     */
    public Result findHotels(final String location, final String description) {
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

        SearchQuery query = new SearchQuery("hotels", fts)
                .limit(100);

        logQuery(query.export().toString());
        SearchQuery searchQuery = new SearchQuery("hotel_fts", fts);
        SearchResult result = cluster.searchQuery(searchQuery);

        //prepare the context to send to the app
        String ftsContext;
        try {
            ftsContext = JacksonTransformers.MAPPER.writerWithDefaultPrettyPrinter().
                    writeValueAsString(query.export());
        } catch (JsonProcessingException e) {
            ftsContext = query.export().toString();
        }
        String subdocContext = "DocumentFragment<Lookup> fragment = bucket\n" +
                "                    .lookupIn(row.id())\n" +
                "                    .get(\"country\")\n" +
                "                    .get(\"city\")\n" +
                "                    .get(\"state\")\n" +
                "                    .get(\"address\")\n" +
                "                    .get(\"name\")\n" +
                "                    .get(\"description\")\n" +
                "                    .execute();";

        return Result.of(extractResultOrThrow(result), ftsContext, subdocContext);
    }

    /**
     * Search for an hotel.
     */
    public Result findHotels(final String description) {
        return findHotels("*", description);
    }

    /**
     * Find all hotels.
     */
    public Result findAllHotels() {
        return findHotels("*", "*");
    }

    /**
     * Extract a FTS result or throw if there is an issue.
     */
    private List<Map<String, Object>> extractResultOrThrow(SearchResult result) {
        if (!result.meta().status().isSuccess()) {
            LOGGER.warn("Query returned with errors: " + result.errors());
            throw new DataRetrievalFailureException("Query error: " + result.errors());
        }

        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        for (SearchQueryRow row : result.rows()) {
            //DocumentFragment<Lookup> fragment = bucket
            Optional<GetResult> result1 = bucket.defaultCollection().get(row.id());
            JsonObject json = result1.get().contentAsObject();

            String country = json.getString("country");
            String city = json.getString("city");
            String state = json.getString("state");
            String address = json.getString("address");
            Map<String, Object> map = new HashMap<String, Object>();

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

            map.put("name", json.getString("name"));
            map.put("description", json.getString("description"));
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
