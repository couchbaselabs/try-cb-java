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
package trycb.util;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.couchbase.client.java.query.Index.PRIMARY_NAME;
import static com.couchbase.client.java.query.Index.createIndex;
import static com.couchbase.client.java.query.Index.createPrimaryIndex;
import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

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
     * Since Couchbase Server 4.0 GA, this should always be skipped since the index definitions are part of the sample.
     */
    private void ensureIndexes() throws Exception {
        LOGGER.info("Ensuring all Indexes are created.");

        N1qlQueryResult indexResult = bucket.query(
            N1qlQuery.simple(select("indexes.*").from("system:indexes").where(i("keyspace_id").eq(s(bucket.name()))))
        );


        List<String> indexesToCreate = new ArrayList<String>();
        indexesToCreate.addAll(Arrays.asList(
            "def_sourceairport", "def_airportname", "def_type", "def_faa", "def_icao", "def_city"
        ));

        boolean hasPrimary = false;
        List<String> foundIndexes = new ArrayList<String>();
        for (N1qlQueryRow indexRow : indexResult) {
            String name = indexRow.value().getString("name");
            Boolean isPrimary = indexRow.value().getBoolean("is_primary");
            if (name.equals(PRIMARY_NAME) || isPrimary == Boolean.TRUE) {
                hasPrimary = true;
            } else {
                foundIndexes.add(name);
            }
        }
        indexesToCreate.removeAll(foundIndexes);

        if (!hasPrimary) {
            //will create the primary index with default name "#primary".
            //Note that some tools may also create it under the name "def_primary" (in which case hasPrimary should be true).
            Statement query = createPrimaryIndex().on(bucket.name()).withDefer();
            LOGGER.info("Executing index query: {}", query);
            N1qlQueryResult result = bucket.query(N1qlQuery.simple(query));
            if (result.finalSuccess()) {
                LOGGER.info("Successfully created primary index.");
            } else {
                LOGGER.warn("Could not create primary index: {}", result.errors());
            }
        }

        for (String name : indexesToCreate) {
            Statement query = createIndex(name).on(bucket.name(), x(name.replace("def_", ""))).withDefer();
            LOGGER.info("Executing index query: {}", query);
            N1qlQueryResult result = bucket.query(N1qlQuery.simple(query));
            if (result.finalSuccess()) {
                LOGGER.info("Successfully created index with name {}.", name);
            } else {
                LOGGER.warn("Could not create index {}: {}", name, result.errors());
            }
        }

        //prepare the list of indexes to build (both primary and secondary indexes)
        List<String> indexesToBuild = new ArrayList<String>(indexesToCreate.size()+1);
        indexesToBuild.addAll(indexesToCreate);
        if (!hasPrimary) {
            indexesToBuild.add(PRIMARY_NAME);
        }

        //skip the build step if all indexes have been found
        if (indexesToBuild.isEmpty()) {
            LOGGER.info("All indexes are already in place, nothing to build");
            return;
        }

        LOGGER.info("Waiting 5 seconds before building the indexes.");
        Thread.sleep(5000);

        //trigger the build
        StringBuilder indexes = new StringBuilder();
        boolean first = true;
        for (String name : indexesToBuild) {
            if (first) {
                first = false;
            } else {
                indexes.append(",");
            }
            indexes.append(name);
        }

        String query = "BUILD INDEX ON `" + bucket.name() + "` (" + indexes.toString() + ")";
        LOGGER.info("Executing index query: {}", query);
        N1qlQueryResult result = bucket.query(N1qlQuery.simple(query));
        if (result.finalSuccess()) {
            LOGGER.info("Successfully executed build index query.");
        } else {
            LOGGER.warn("Could not execute build index query {}.", result.errors());
        }
    }

}
