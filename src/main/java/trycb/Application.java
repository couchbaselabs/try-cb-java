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
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.json.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class Application implements Filter {

    // ======
    // Config Options
    // ======

    @Value("${hostname}")
    private String hostname;

    @Value("${bucket}")
    private String bucket;

    @Value("${password}")
    private String password;

    // ======
    // Bean Definitions
    // ======

    public @Bean Cluster cluster() {
        return CouchbaseCluster.create(hostname);
    }

    public @Bean Bucket bucket() {
        return cluster().openBucket(bucket, password);
    }

    // ======
    // HTTP Routes
    // ======

    @RequestMapping("/airport/findAll")
    public List<Map<String, Object>> airports(@RequestParam String search) {
        return Database.findAllAirports(bucket(), search);
    }

    @RequestMapping("/flightPath/findAll")
    public List<Map<String, Object>> all(@RequestParam String from, @RequestParam String to, @RequestParam String leave)
        throws Exception {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.setTime(DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).parse(leave));
        return Database.findAllFlightPaths(bucket(), from, to, calendar);
    }

    @RequestMapping(value="/user/login", method=RequestMethod.GET)
    public Object login(@RequestParam String user, @RequestParam String password) {
        return Database.login(bucket(), user, password);
    }

    @RequestMapping(value="/user/login", method=RequestMethod.POST)
    public Object createLogin(@RequestBody String json) {
        JsonObject jsonData = JsonObject.fromJson(json);
        return Database.createLogin(bucket(), jsonData.getString("user"), jsonData.getString("password"));
    }

    @RequestMapping(value="/user/flights", method=RequestMethod.POST)
    public Object book(@RequestBody String json) {
        JsonObject jsonData = JsonObject.fromJson(json);
        return Database.flights(bucket(), jsonData.getString("token"), jsonData.getArray("flights").toString());
    }

    @RequestMapping(value="/user/flights", method=RequestMethod.GET)
    public Object booked(@RequestParam String token) {
        return Database.getFlights(bucket(), token);
    }

    // ======
    // Main Method
    // ======

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // ======
    // Enable CORS
    // ======

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}

}
