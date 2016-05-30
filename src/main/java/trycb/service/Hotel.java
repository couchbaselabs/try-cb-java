package trycb.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.search.HighlightStyle;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.queries.AbstractFtsQuery;
import com.couchbase.client.java.search.result.SearchQueryResult;
import com.couchbase.client.java.search.result.SearchQueryRow;
import com.couchbase.client.java.subdoc.DocumentFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

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
    public List<Map<String, Object>> findHotels(final String location, final String description) {
        AbstractFtsQuery fts = SearchQuery.conjuncts(
                SearchQuery.term("landmark").field("type"),
                SearchQuery.disjuncts(
                        SearchQuery.matchPhrase(location).field("country"),
                        SearchQuery.matchPhrase(location).field("city"),
                        SearchQuery.matchPhrase(location).field("state"),
                        SearchQuery.matchPhrase(location).field("address")
                ),
                SearchQuery.matchPhrase(description).field("content"),
                SearchQuery.match("hotel").field("content"));
        SearchQuery query = new SearchQuery("travel-search", fts);

        logQuery(query.export().toString());
        SearchQueryResult result = bucket.query(query);
        return extractResultOrThrow(result);
    }

    /**
     * Search for an hotel.
     */
    public List<Map<String, Object>> findHotels(final String description) {
        AbstractFtsQuery fts = SearchQuery.conjuncts(
                SearchQuery.term("landmark").field("type"),
                SearchQuery.matchPhrase(description).field("content"),
                SearchQuery.match("hotel").field("content"));
        SearchQuery query = new SearchQuery("travel-search", fts);

        logQuery(query.export().toString());
        SearchQueryResult result = bucket.query(query);
        return extractResultOrThrow(result);
    }

    /**
     * Find all hotels.
     */
    public List<Map<String, Object>> findAllHotels() {
        AbstractFtsQuery fts = SearchQuery.conjuncts(
                SearchQuery.term("landmark").field("type"),
                SearchQuery.match("hotel").field("content"));
        SearchQuery query = new SearchQuery("travel-search", fts);

        logQuery(query.export().toString());
        SearchQueryResult result = bucket.query(query);
        return extractResultOrThrow(result);
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
