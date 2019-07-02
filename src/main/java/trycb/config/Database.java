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
package trycb.config;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
//import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Database {

    @Value("${storage.host}")
    private String host;

    @Value("${storage.bucket}")
    private String bucket;

    @Value("${storage.username}")
    private String username;

    @Value("${storage.password}")
    private String password;

    public @Bean Cluster couchbaseCluster() {
        //CouchbaseCluster cluster = CouchbaseCluster.create(host);
        Cluster cluster = Cluster.connect(host, username, password);
        //cluster.authenticate(username, password);
        return cluster;
    }

    public @Bean Bucket loginBucket() {
        //return couchbaseCluster().openBucket(bucket);
        return couchbaseCluster().bucket(bucket);
    }

    public @Bean Collection loginCollection() {
        //return couchbaseCluster().openBucket(bucket);
        return couchbaseCluster().bucket((bucket)).defaultCollection();
    }

}
