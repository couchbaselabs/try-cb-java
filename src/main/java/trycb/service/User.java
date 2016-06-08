package trycb.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import rx.functions.Func1;

@Service
public class User {

    /**
     * Try to log the given user in.
     */
    public static Map<String, Object> login(final Bucket bucket, final String username, final String password) {
        JsonDocument doc = bucket.get("user::" + username);

        if (doc == null) {
            throw new AuthenticationCredentialsNotFoundException("Bad Username or Password");
        } else if(BCrypt.checkpw(password, doc.content().getString("password"))) {
            return doc.content().toMap();
        } else {
            throw new AuthenticationCredentialsNotFoundException("Bad Username or Password");
        }
    }

    /**
     * Create a user.
     */
    public static Map<String, Object> createLogin(final Bucket bucket, final String username, final String password) {
        String passHash = BCrypt.hashpw(password, BCrypt.gensalt());
        JsonObject data = JsonObject.create()
            .put("type", "user")
            .put("name", username)
            .put("password", passHash)
            .put("token", passHash);
        JsonDocument doc = JsonDocument.create("user::" + username, data);

        try {
            bucket.insert(doc);
            return data.toMap();
        } catch (Exception e) {
            throw new AuthenticationServiceException("There was an error creating account");
        }
    }

    /**
     * Register a flight (or flights) for the given user.
     */
    public static Map<String, Object> registerFlightForUser(final Bucket bucket, final String username, final JsonArray newFlights) {
        JsonDocument userData = bucket.get("user::" + username);
        if (userData == null) {
            throw new IllegalStateException("A user needs to be created first.");
        }

        JsonArray allBookedFlights = userData.content().getArray("flights");
        if(allBookedFlights == null) {
            allBookedFlights = JsonArray.create();
        }

        for (Object newFlight : newFlights) {
            JsonObject t = ((JsonObject) newFlight).getObject("_data");
            JsonObject flightJson = JsonObject.empty()
                .put("name", t.get("name"))
                .put("flight", t.get("flight"))
                .put("date", t.get("date"))
                .put("sourceairport", t.get("sourceairport"))
                .put("destinationairport", t.get("destinationairport"))
                .put("bookedon", "");
            allBookedFlights.add(flightJson);
        }

        userData.content().put("flights", allBookedFlights);
        JsonDocument response = bucket.upsert(userData);
        JsonObject responseData = JsonObject.create()
            .put("added", response.content().getArray("flights"));
        return responseData.toMap();
    }

    /**
     * Show all booked flights for the given user.
     */
    public static List<Object> getFlightsForUser(final Bucket bucket, final String username) {
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
                     .single()
                     .toBlocking()
                     .single();
    }
}
