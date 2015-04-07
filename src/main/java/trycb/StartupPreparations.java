package trycb;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;

@Component
public class StartupPreparations implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupPreparations.class);

    private final Bucket bucket;

    @Autowired
    public StartupPreparations(final Bucket bucket) {
        this.bucket = bucket;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ensureIndexes();
    }

    /**
     * Helper method to ensure all indexes are created for this application to run properly.
     */
    private void ensureIndexes() {
        LOGGER.info("Ensuring all Indexes are created.");

        QueryResult indexResult = bucket.query(
            Query.simple(select("indexes.*").from("system:indexes").where(i("keyspace_id").eq(s(bucket.name()))))
        );


        List<String> indexesToCreate = new ArrayList<String>();
        indexesToCreate.addAll(Arrays.asList(
            "def_sourceairport", "def_airportname", "def_type", "def_faa", "def_icao", "def_city"
        ));

        boolean hasPrimary = false;
        List<String> foundIndexes = new ArrayList<String>();
        for (QueryRow indexRow : indexResult) {
            String name = indexRow.value().getString("name");
            if (name.equals("#primary")) {
                hasPrimary = true;
            } else {
                foundIndexes.add(name);
            }
        }
        indexesToCreate.removeAll(foundIndexes);

        if (!hasPrimary) {
            String query = "CREATE PRIMARY INDEX ON `" + bucket.name() + "`";
            LOGGER.info("Executing index query: {}", query);
            QueryResult result = bucket.query(Query.simple(query));
            if (result.finalSuccess()) {
                LOGGER.info("Successfully created primary index.");
            } else {
                LOGGER.warn("Could not create primary index: {}", result.errors());
            }
        }

        for (String name : indexesToCreate) {
            String query = "CREATE INDEX " + name + " ON `" + bucket.name() + "` (" + name.replace("def_", "") + ");";
            LOGGER.info("Executing index query: {}", query);
            QueryResult result = bucket.query(Query.simple(query));
            if (result.finalSuccess()) {
                LOGGER.info("Successfully created index with name {}.", name);
            } else {
                LOGGER.warn("Could not create index {}: {}", name, result.errors());
            }
        }
    }

}
