package trycb.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.couchbase.client.core.msg.kv.DurabilityLevel;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
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

    static final String USERS_COLLECTION_NAME = "users";
    static final String FLIGHTS_COLLECTION_NAME = "flights";

    /**
     * Try to log the given user in.
     */
    public Map<String, Object> login(final Scope scope, final String username, final String password) {
        Optional<GetResult> doc = scope.collection(USERS_COLLECTION_NAME).get(username);

        if (!doc.isPresent()) {
            throw new AuthenticationCredentialsNotFoundException("Bad Username or Password: " + username);
        }
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
    public Result<Map<String, Object>> createLogin(final Scope scope, final String username, final String password,
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
        String narration = "User account created in document " + username + " in bucket " + scope.bucketName()
                + " scope " + scope.name() + " collection " + USERS_COLLECTION_NAME
                + (expiry.ordinal() > 0 ? ", with expiry of " + expiry.ordinal() + "s" : "");

        try {
            scope.collection(USERS_COLLECTION_NAME).insert(username, doc);
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
    public Result<Map<String, Object>> registerFlightForUser(final Scope scope, final String username, final JsonArray newFlights) {
        String userId = username;
        Collection usersCollection = scope.collection(USERS_COLLECTION_NAME);
        Collection flightsCollection = scope.collection(FLIGHTS_COLLECTION_NAME);
        Optional<GetResult> userDataFetch = usersCollection.get(userId);
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
            String flightId = UUID.randomUUID().toString();
            flightsCollection.insert(flightId, t);
            allBookedFlights.add(flightId);
            added.add(t);
        }

        userData.put("flights", allBookedFlights);
        usersCollection.upsert(userId, userData);

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

    public List<Map<String, Object>> getFlightsForUser(final Scope scope, final String username) {
        Collection users = scope.collection(USERS_COLLECTION_NAME);
        Optional<GetResult> doc = users.get(username);
        if (!doc.isPresent()) {
            return Collections.emptyList();
        }
        JsonObject data = doc.get().contentAsObject();
        JsonArray flights = data.getArray("flights");
        if (flights == null) {
            return Collections.emptyList();
        }

        // The "flights" array contains flight ids. Convert them to actual objects.
        Collection flightsCollection = scope.collection(FLIGHTS_COLLECTION_NAME);
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < flights.size(); i++) {
            String flightId = flights.getString(i);
            Optional<GetResult> res = flightsCollection.get(flightId);
            if (!res.isPresent()) {
                throw new RuntimeException("Unable to retrieve flight id " + flightId);
            }
            Map<String, Object> flight = res.get().contentAsObject().toMap();
            results.add(flight);
        }
        return results;
    }

}
