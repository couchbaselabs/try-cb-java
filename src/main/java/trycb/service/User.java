package trycb.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import rx.functions.Func1;
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
        JsonDocument doc = bucket.get("user::" + username);

        if (doc == null) {
            throw new AuthenticationCredentialsNotFoundException("Bad Username or Password");
        } else if(BCrypt.checkpw(password, doc.content().getString("password"))) {
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
        JsonDocument doc;
        if (expiry > 0) {
            doc = JsonDocument.create("user::" + username, expiry, data);
        } else {
            doc = JsonDocument.create("user::" + username, data);
        }
        String narration = "User account created in document " + doc.id() + " in " + bucket.name()
                + (doc.expiry() > 0 ? ", with expiry of " + doc.expiry() + "s" : "");

        try {
            bucket.insert(doc);
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
        JsonDocument userData = bucket.get("user::" + username);
        if (userData == null) {
            throw new IllegalStateException();
        }

        if (newFlights == null) {
            throw new IllegalArgumentException("No flights in payload");
        }

        JsonArray added = JsonArray.empty();
        JsonArray allBookedFlights = userData.content().getArray("flights");
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

        userData.content().put("flights", allBookedFlights);
        JsonDocument response = bucket.upsert(userData);

        JsonObject responseData = JsonObject.create()
            .put("added", added);

        return Result.of(responseData.toMap(), "Booked flight in Couchbase document " + response.id());
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
    public List<Object> getFlightsForUser(final Bucket bucket, final String username) {
        return bucket.async()
                     .get("user::" + username)
                     .map(new Func1<JsonDocument, List<Object>>() {
                         @Override
                         public List<Object> call(JsonDocument doc) {
                             JsonObject data = doc.content();
                             JsonArray flights = data.getArray("flights");
                             if (flights != null) {
                                 return flights.toList();
                             } else {
                                 return Collections.emptyList();
                             }
                         }
                     })
                     .defaultIfEmpty(Collections.emptyList())
                     .toBlocking()
                     .single();
    }
}
