package trycb.service;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;


import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.kv.*;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import trycb.model.Result;

@Service
public class User {

    private final TokenService jwtService;

    @Autowired
    public User(TokenService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Try to log the given user in.
     */
    public Map<String, Object> login(final Bucket bucket, final String username, final String password) {
        Optional<GetResult> doc = bucket.defaultCollection().get(username);

        if (!doc.isPresent()){
            throw new AuthenticationCredentialsNotFoundException("Bad Username or Password");
        } else if(BCrypt.checkpw(password, doc.get().contentAsObject().getString("password"))) {
            return JsonObject.create()
                .put("token", jwtService.buildToken(username))
                .toMap();
        } else {
            throw new AuthenticationCredentialsNotFoundException("Bad Username or Password");
        }
    }

    /**
     * Create a user.
     */
    public Result<Map<String, Object>> createLogin(final Bucket bucket, final String username, final String password,
            int expiry) {
        String passHash = BCrypt.hashpw(password, BCrypt.gensalt());
        JsonObject data = JsonObject.create()
            .put("type", "user")
            .put("name", username)
            .put("password", passHash);
        JsonObject doc;
        doc = JsonObject.create();
            doc.put("user", username);
            doc.put("data", data);

        String narration = "User account created in document " + username + " in " + bucket.name()
                + (expiry > 0 ? ", with expiry of " + expiry + " minute(s)" : "");

        try {
            if (expiry > 0) {
                bucket.defaultCollection().insert(username, doc, InsertOptions.insertOptions().expiry(Duration.ofMinutes(expiry)));
            }
            else {
                bucket.defaultCollection().insert(username, doc);
            }
            return Result.of(
                    JsonObject.create().put("token", jwtService.buildToken(username)).toMap(),
                    narration);
        } catch (Exception e) {
            throw new AuthenticationServiceException("There was an error creating account");
        }
    }

    /**
     * Register a flight (or flights) for the given user.
     */
    public Result<Map<String, Object>> registerFlightForUser(final Bucket bucket, final String username, final JsonArray newFlights) {
        Optional<GetResult> userDataProbe = bucket.defaultCollection().get(username);
        GetResult userData = null;
        if (!userDataProbe.isPresent()) {
            throw new IllegalStateException();
        }else{
            userData = userDataProbe.get();
        }

        if (newFlights == null) {
            throw new IllegalArgumentException("No flights in payload");
        }

        JsonArray added = JsonArray.empty();
        JsonArray allBookedFlights = userData.contentAsObject().getArray("flights");
        if(allBookedFlights == null) {
            allBookedFlights = JsonArray.create();
        }

        for (Object newFlight : newFlights) {
            checkFlight(newFlight);
            JsonObject t = ((JsonObject) newFlight);
            t.put("bookedon", "try-cb-java");
            allBookedFlights.add(t);
            added.add(t);
        }

        userData.contentAsObject().put("flights", allBookedFlights);
        MutationResult response = bucket.defaultCollection().upsert(username, userData.contentAsObject());

        JsonObject responseData = JsonObject.create()
            .put("added", added);

        return Result.of(responseData.toMap(), "Booked flight in Couchbase document " + response.toString());
    }

    private static void checkFlight(Object f) {
        if (f == null || !(f instanceof JsonObject)) {
            throw new IllegalArgumentException("Each flight must be a non-null object");
        }
        JsonObject flight = (JsonObject) f;
        if (!flight.containsKey("name")
                || !flight.containsKey("date")
                || !flight.containsKey("sourceairport")
                || !flight.containsKey("destinationairport")) {
            throw new IllegalArgumentException("Malformed flight inside flights payload");
        }
    }

    /**
     * Show all booked flights for the given user.
     */
    public Object getFlightsForUser(final Bucket bucket, final String username) {
        Object x = bucket.defaultCollection()
                .get(username)
                .get()
                .contentAsObject()
                .getArray("flights");
        if(x == null)
            return Collections.emptyList();
        return x;
    }
}
