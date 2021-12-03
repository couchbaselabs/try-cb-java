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

import java.nio.file.Paths;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.core.env.SecurityConfig;
import com.couchbase.client.core.env.IoConfig;
import com.couchbase.client.java.ClusterOptions;

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
    
    @Value("${storage.dnssrv}")
    private Boolean dnssrv;
    
    @Value("${storage.cert}")
    private String cert; // blank means no cert

    public @Bean Cluster loginCluster() {
        
        ClusterEnvironment.Builder envBuilder
            = ClusterEnvironment.builder();
            
        if (dnssrv) {
            envBuilder.ioConfig(IoConfig.enableDnsSrv(true));
        }
        
        if (cert != "") {
            envBuilder.securityConfig(SecurityConfig
                .enableTls(true)
                .trustCertificate(Paths.get(cert)));
            }
        }
            
        ClusterEnvironment env = envBuilder.build();

        return Cluster.connect(
            host,
            ClusterOptions
                .clusterOptions(username, password)
                .environment(env));
    }

    public @Bean Bucket loginBucket() {
        return loginCluster().bucket(bucket);
    }

}
