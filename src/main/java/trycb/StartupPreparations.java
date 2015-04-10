/**
 * Copyright (C) 2015 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
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
    private void ensureIndexes() throws Exception {
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
            String query = "CREATE PRIMARY INDEX def_primary ON `" + bucket.name() + "` USING gsi WITH {\"defer_build\":true}";
            LOGGER.info("Executing index query: {}", query);
            QueryResult result = bucket.query(Query.simple(query));
            if (result.finalSuccess()) {
                LOGGER.info("Successfully created primary index.");
            } else {
                LOGGER.warn("Could not create primary index: {}", result.errors());
            }
        }

        for (String name : indexesToCreate) {
            String query = "CREATE INDEX " + name + " ON `" + bucket.name() + "` (" + name.replace("def_", "") + ") "
                + "USING gsi WITH {\"defer_build\":true}\"";
            LOGGER.info("Executing index query: {}", query);
            QueryResult result = bucket.query(Query.simple(query));
            if (result.finalSuccess()) {
                LOGGER.info("Successfully created index with name {}.", name);
            } else {
                LOGGER.warn("Could not create index {}: {}", name, result.errors());
            }
        }

        LOGGER.info("Waiting 5 seconds before building the indexes.");

        Thread.sleep(5000);

        StringBuilder indexes = new StringBuilder();
        boolean first = true;
        for (String name : indexesToCreate) {
            if (first) {
                first = false;
            } else {
                indexes.append(",");
            }
            indexes.append(name);
        }

        if (!hasPrimary) {
            indexes.append(",").append("def_primary");
        }

        String query = "BUILD INDEX ON `" + bucket.name() + "` (" + indexes.toString() + ") USING GSI";
        LOGGER.info("Executing index query: {}", query);
        QueryResult result = bucket.query(Query.simple(query));
        if (result.finalSuccess()) {
            LOGGER.info("Successfully executed build index query.");
        } else {
            LOGGER.warn("Could not execute build index query {}.", result.errors());
        }
    }

}
