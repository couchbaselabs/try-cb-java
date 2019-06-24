package trycb.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.couchbase.client.core.msg.kv.DurabilityLevel;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.InsertOptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import trycb.model.Result;

import static com.couchbase.client.java.kv.InsertOptions.insertOptions;


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
        Optional<GetResult> doc = bucket.defaultCollection().get("user::" + username);

        if (!doc.isPresent())
            throw new AuthenticationCredentialsNotFoundException("Bad Username or Password");
        JsonObject res = doc.get().contentAsObject();
        if(BCrypt.checkpw(password, res.getString("password"))) {
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
            DurabilityLevel expiry) {
        String passHash = BCrypt.hashpw(password, BCrypt.gensalt());
        JsonObject doc = JsonObject.create()
            .put("type", "user")
            .put("name", username)
            .put("password", passHash);
        InsertOptions options = insertOptions();
        if (expiry.ordinal() > 0) {
            options.durabilityLevel(expiry);
        }
        String narration = "User account created in document user::" + username + " in " + bucket.name()
                + (expiry.ordinal() > 0 ? ", with expiry of " + expiry.ordinal() + "s" : "");

        try {
            bucket.defaultCollection().insert("user::" + username, doc);
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
        String userId = "user::" + username;
        Optional<GetResult> userDataFetch = bucket.defaultCollection().get(userId);
        if (!userDataFetch.isPresent()) {
            throw new IllegalStateException();
        }
        JsonObject userData = userDataFetch.get().contentAsObject();

        if (newFlights == null) {
            throw new IllegalArgumentException("No flights in payload");
        }

        JsonArray added = JsonArray.empty();
        JsonArray allBookedFlights = userData.getArray("flights");
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

        userData.put("flights", allBookedFlights);
        bucket.defaultCollection().upsert(userId, userData);

        JsonObject responseData = JsonObject.create()
            .put("added", added);

        return Result.of(responseData.toMap(), "Booked flight in Couchbase document " + userId);
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
        try {
            return bucket.defaultCollection()
                         .async()
                         .get("user::" + username)
                         .thenApply(new Function<Optional<GetResult>, List<Object>>() {
                             @Override
                             public List<Object> apply(Optional<GetResult> doc) {
                                 if (!doc.isPresent()) {
                                     return Collections.emptyList();
                                 }
                                 JsonObject data = doc.get().contentAsObject();
                                 JsonArray flights = data.getArray("flights");
                                 if (flights != null) {
                                     return flights.toList();
                                 } else {
                                     return Collections.emptyList();
                                 }
                             }
                         })
                         .get();
        // FIXME: What is the right thing to do in the event of an exception here?
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to get flights for user " + username, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Unable to get flights for user " + username, e);
        }
    }
}
