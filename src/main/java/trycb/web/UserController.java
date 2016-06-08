package trycb.web;


import java.util.List;
import java.util.Map;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trycb.model.Error;
import trycb.model.IValue;
import trycb.model.Result;
import trycb.service.User;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final Bucket bucket;

    @Autowired
    public UserController(Bucket bucket) {
        this.bucket = bucket;
    }

    @RequestMapping(value="/login/{user}/{password}", method= RequestMethod.GET)
    public ResponseEntity<? extends IValue> login(@PathVariable("user") String user, @PathVariable("password") String password) {
        try {
            Map<String, Object> data = User.login(bucket, user, password);
            return ResponseEntity.ok(Result.of(data));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new Error(e.getMessage()));
        }
    }

    @RequestMapping(value="/login", method=RequestMethod.POST)
    public ResponseEntity<? extends IValue> createLogin(@RequestBody String json) {
        JsonObject jsonData = JsonObject.fromJson(json);
        try {
            Map<String, Object> data = User.createLogin(bucket, jsonData.getString("user"), jsonData.getString("password"));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Result.of(data));
        } catch (AuthenticationServiceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new Error(e.getMessage()));
        }
    }

    @RequestMapping(value="/flights", method=RequestMethod.POST)
    public ResponseEntity<? extends IValue> book(@RequestBody String json) {
        JsonObject jsonData = JsonObject.fromJson(json);
        if (!jsonData.containsKey("username")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Error("A username must be provided"));
        }

        try {
            Map<String, Object> added = User.registerFlightForUser(bucket, jsonData.getString("username"), jsonData.getArray("flights"));
            return ResponseEntity.accepted()
                    .body(Result.of(added));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Error("Forbidden, you can't book for this user"));
        } catch(IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Error(e.getMessage()));
        }

    }

    @RequestMapping(value="/flights", method=RequestMethod.GET)
    public Object booked(@RequestParam String username) {
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Error("A username must be provided"));
        }

        try {
            List<Object> flights = User.getFlightsForUser(bucket, username);
            return ResponseEntity.ok(Result.of(flights));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Error("Forbidden, you don't have access to this cart"));
        }
    }

}
