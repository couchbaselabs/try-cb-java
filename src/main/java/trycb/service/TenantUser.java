package trycb.service;

import static com.couchbase.client.java.kv.InsertOptions.insertOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.core.msg.kv.DurabilityLevel;
import com.couchbase.client.java.Bucket;
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

@Service
public class TenantUser {

    private final TokenService jwtService;

    @Autowired
    public TenantUser(TokenService jwtService) {
        this.jwtService = jwtService;
    }

    static final String USERS_COLLECTION_NAME = "users";
    static final String BOOKINGS_COLLECTION_NAME = "bookings";

    /**
     * Try to log the given tenant user in.
     */
    public Result<Map<String, Object>> login(final Bucket bucket, final String tenant, final String username,
            final String password) {
        Scope scope = bucket.scope(tenant);
        Collection collection = scope.collection(USERS_COLLECTION_NAME);
        String queryType = String.format("KV get - scoped to %s.users: for password field in document %s", scope.name(),
                username);

        GetResult doc;
        try {
            doc = collection.get(username);
        } catch (DocumentNotFoundException ex) {
            throw new AuthenticationCredentialsNotFoundException("Bad Username or Password");
        }
        JsonObject res = doc.contentAsObject();
        if (BCrypt.checkpw(password, res.getString("password"))) {
            Map<String, Object> data = JsonObject.create()
                    .put("token", jwtService.buildToken(username))
                    .toMap();
            return Result.of(data, queryType);
        } else {
            throw new AuthenticationCredentialsNotFoundException("Bad Username or Password");
        }
    }

    /**
     * Create a tenant user.
     */
    public Result<Map<String, Object>> createLogin(final Bucket bucket, final String tenant, final String username,
            final String password, DurabilityLevel expiry) {
        String passHash = BCrypt.hashpw(password, BCrypt.gensalt());
        JsonObject doc = JsonObject.create()
                .put("type", "user")
                .put("name", username)
                .put("password", passHash);
        InsertOptions options = insertOptions();
        if (expiry.ordinal() > 0) {
            options.durability(expiry);
        }

        Scope scope = bucket.scope(tenant);
        Collection collection = scope.collection(USERS_COLLECTION_NAME);
        String queryType = String.format("KV insert - scoped to %s.users: document %s", scope.name(), username);
        try {
            collection.insert(username, doc, options);
            Map<String, Object> data = JsonObject.create().put("token", jwtService.buildToken(username)).toMap();
            return Result.of(data, queryType);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AuthenticationServiceException("There was an error creating account");
        }
    }

    /*
     * Register a flight (or flights) for the given tenant user.
     */
    public Result<Map<String, Object>> registerFlightForUser(final Bucket bucket, final String tenant,
            final String username, final JsonArray newFlights) {
        String userId = username;
        GetResult userDataFetch;
        Scope scope = bucket.scope(tenant);
        Collection usersCollection = scope.collection(USERS_COLLECTION_NAME);
        Collection bookingsCollection = scope.collection(BOOKINGS_COLLECTION_NAME);

        try {
            userDataFetch = usersCollection.get(userId);
        } catch (DocumentNotFoundException ex) {
            throw new IllegalStateException();
        }
        JsonObject userData = userDataFetch.contentAsObject();

        if (newFlights == null) {
            throw new IllegalArgumentException("No flights in payload");
        }

        JsonArray added = JsonArray.create();
        JsonArray allBookedFlights = userData.getArray("flights");
        if (allBookedFlights == null) {
            allBookedFlights = JsonArray.create();
        }

        for (Object newFlight : newFlights) {
            checkFlight(newFlight);
            JsonObject t = ((JsonObject) newFlight);
            t.put("bookedon", "try-cb-java");
            String flightId = UUID.randomUUID().toString();
            bookingsCollection.insert(flightId, t);
            allBookedFlights.add(flightId);
            added.add(t);
        }

        userData.put("flights", allBookedFlights);
        usersCollection.upsert(userId, userData);

        JsonObject responseData = JsonObject.create().put("added", added);

        String queryType = String.format("KV update - scoped to %s.users: for bookings field in document %s",
                scope.name(), username);
        return Result.of(responseData.toMap(), queryType);
    }

    private static void checkFlight(Object f) {
        if (f == null || !(f instanceof JsonObject)) {
            throw new IllegalArgumentException("Each flight must be a non-null object");
        }
        JsonObject flight = (JsonObject) f;
        if (!flight.containsKey("name") || !flight.containsKey("date") || !flight.containsKey("sourceairport")
                || !flight.containsKey("destinationairport")) {
            throw new IllegalArgumentException("Malformed flight inside flights payload" + flight.toString());
        }
    }

    public Result<List<Map<String, Object>>> getFlightsForUser(final Bucket bucket, final String tenant,
            final String username) {
        GetResult userDoc;
        Scope scope = bucket.scope(tenant);
        Collection usersCollection = scope.collection(USERS_COLLECTION_NAME);
        Collection bookingsCollection = scope.collection(BOOKINGS_COLLECTION_NAME);

        try {
            userDoc = usersCollection.get(username);
        } catch (DocumentNotFoundException ex) {
            return Result.of(Collections.emptyList());
        }
        JsonObject userData = userDoc.contentAsObject();
        JsonArray flights = userData.getArray("flights");
        if (flights == null) {
            return Result.of(Collections.emptyList());
        }

        // The "flights" array contains flight ids. Convert them to actual objects.
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < flights.size(); i++) {
            String flightId = flights.getString(i);
            GetResult res;
            try {
                res = bookingsCollection.get(flightId);
            } catch (DocumentNotFoundException ex) {
                throw new RuntimeException("Unable to retrieve flight id " + flightId);
            }
            Map<String, Object> flight = res.contentAsObject().toMap();
            results.add(flight);
        }

        String queryType = String.format("KV get - scoped to %s.users: for %d bookings in document %s", scope.name(),
                results.size(), username);
        return Result.of(results, queryType);
    }

}
