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
package trycb.web;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trycb.data.Dao;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class Controller {

    private final Bucket bucket;

    @Autowired
    public Controller(Bucket bucket) {
        this.bucket = bucket;
    }

    @RequestMapping("/airport/findAll")
    public List<Map<String, Object>> airports(@RequestParam String search) {
        return Dao.findAllAirports(bucket, search);
    }

    @RequestMapping("/flightPath/findAll")
    public List<Map<String, Object>> all(@RequestParam String from, @RequestParam String to, @RequestParam String leave)
        throws Exception {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.setTime(DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).parse(leave));
        return Dao.findAllFlightPaths(bucket, from, to, calendar);
    }

    @RequestMapping(value="/user/login", method= RequestMethod.GET)
    public Object login(@RequestParam String user, @RequestParam String password) {
        return Dao.login(bucket, user, password);
    }

    @RequestMapping(value="/user/login", method=RequestMethod.POST)
    public Object createLogin(@RequestBody String json) {
        JsonObject jsonData = JsonObject.fromJson(json);
        return Dao.createLogin(bucket, jsonData.getString("user"), jsonData.getString("password"));
    }

    @RequestMapping(value="/user/flights", method=RequestMethod.POST)
    public Object book(@RequestBody String json) {
        JsonObject jsonData = JsonObject.fromJson(json);
        return Dao.registerFlightForUser(bucket, jsonData.getString("username"), jsonData.getArray("flights"));
    }

    @RequestMapping(value="/user/flights", method=RequestMethod.GET)
    public Object booked(@RequestParam String username) {
        return Dao.getFlightsForUser(bucket, username);
    }

}
