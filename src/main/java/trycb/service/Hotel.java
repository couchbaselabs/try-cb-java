package trycb.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.queries.ConjunctionQuery;
import com.couchbase.client.java.search.result.SearchQueryResult;
import com.couchbase.client.java.search.result.SearchQueryRow;
import com.couchbase.client.java.subdoc.DocumentFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import trycb.model.Result;

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
    public Result findHotels(final String location, final String description) {
        ConjunctionQuery fts = SearchQuery.conjuncts(
                SearchQuery.term("landmark").field("type"),
                SearchQuery.match("hotel").field("content"));

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
                        SearchQuery.matchPhrase(description).field("content"),
                        SearchQuery.matchPhrase(description).field("name")
                ));
        }

        SearchQuery query = new SearchQuery("travel-search", fts)
                .limit(100);

        logQuery(query.export().toString());
        SearchQueryResult result = bucket.query(query);


        String subdocContext = "DocumentFragment<Lookup> fragment = bucket\n" +
                "                    .lookupIn(row.id())\n" +
                "                    .get(\"country\")\n" +
                "                    .get(\"city\")\n" +
                "                    .get(\"state\")\n" +
                "                    .get(\"address\")\n" +
                "                    .get(\"name\")\n" +
                "                    .get(\"content\")\n" +
                "                    .execute();";
        List<Map<String, Object>> data = extractResultOrThrow(result);
        return new Result(data, query.export().toString(), subdocContext);
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
    private List<Map<String, Object>> extractResultOrThrow(SearchQueryResult result) {
        if (!result.status().isSuccess()) {
            LOGGER.warn("Query returned with errors: " + result.errors());
            throw new DataRetrievalFailureException("Query error: " + result.errors());
        }

        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        for (SearchQueryRow row : result) {
            DocumentFragment<Lookup> fragment = bucket
                    .lookupIn(row.id())
                    .get("country")
                    .get("city")
                    .get("state")
                    .get("address")
                    .get("name")
                    .get("content")
                    .execute();

            Map<String, Object> map = new HashMap<String, Object>();

            String country = (String) fragment.content("country");
            String city = (String) fragment.content("city");
            String state = (String) fragment.content("state");
            String address = (String) fragment.content("address");

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

            map.put("name", fragment.content("name"));
            map.put("description", fragment.content("content"));
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
