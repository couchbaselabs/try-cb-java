package trycb.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import rx.functions.Func1;

@Service
public class User {

    /**
     * Try to log the given user in.
     */
    public static ResponseEntity<String> login(final Bucket bucket, final String username, final String password) {
        JsonDocument doc = bucket.get("user::" + username);

        JsonObject responseContent;
        if (doc == null) {
            responseContent = JsonObject.create().put("success", false).put("failure", "Bad Username or Password");
        } else if(BCrypt.checkpw(password, doc.content().getString("password"))) {
            responseContent = JsonObject.create().put("success", true).put("data", doc.content());
        } else {
            responseContent = JsonObject.empty().put("success", false).put("failure", "Bad Username or Password");
        }
        return new ResponseEntity<String>(responseContent.toString(), HttpStatus.OK);
    }

    /**
     * Create a user.
     */
    public static ResponseEntity<String> createLogin(final Bucket bucket, final String username, final String password) {
        JsonObject data = JsonObject.create()
            .put("type", "user")
            .put("name", username)
            .put("password", BCrypt.hashpw(password, BCrypt.gensalt()));
        JsonDocument doc = JsonDocument.create("user::" + username, data);

        try {
            bucket.insert(doc);
            JsonObject responseData = JsonObject.create()
                .put("success", true)
                .put("data", data);
            return new ResponseEntity<String>(responseData.toString(), HttpStatus.OK);
        } catch (Exception e) {
            JsonObject responseData = JsonObject.empty()
                .put("success", false)
                .put("failure", "There was an error creating account")
                .put("exception", e.getMessage());
            return new ResponseEntity<String>(responseData.toString(), HttpStatus.OK);
        }
    }

    /**
     * Register a flight (or flights) for the given user.
     */
    public static ResponseEntity<String> registerFlightForUser(final Bucket bucket, final String username, final JsonArray newFlights) {
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
            .put("added", response.content().getArray("flights").size());
        return new ResponseEntity<String>(responseData.toString(), HttpStatus.OK);
    }

    /**
     * Show all booked flights for the given user.
     */
    public static ResponseEntity<String> getFlightsForUser(final Bucket bucket, final String username) {
        return bucket.async()
                     .get("user::" + username)
                     .map(new Func1<JsonDocument, ResponseEntity<String>>() {
                         @Override
                         public ResponseEntity<String> call(JsonDocument doc) {
                             return new ResponseEntity<String>(doc.content().getArray("flights").toString(), HttpStatus.OK);
                         }
                     })
                     .defaultIfEmpty(new ResponseEntity<String>("{failure: 'No flights found'}", HttpStatus.OK))
                     .toBlocking()
                     .single();
    }
}
