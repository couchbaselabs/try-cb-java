package trycb.flightpath;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryRow;
import com.couchbase.client.java.query.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trycb.Application;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

@RestController
@RequestMapping(Application.URI_PREFIX + "/flightPath")
public class FlightPathController {

    private final Bucket bucket;

    @Autowired
    public FlightPathController(final Bucket bucket) {
        this.bucket = bucket;
    }

    @RequestMapping("findAll")
    public List<Map> all(
        @RequestParam("from") String from,
        @RequestParam("to") String to,
        @RequestParam("leave") String leave
    ) throws Exception {

        Statement query = select(x("faa").as("fromAirport")).from(i(bucket.name())).where(x("airportname").eq(s(from)))
            .union()
            .select(x("faa").as("toAirport")).from(i(bucket.name())).where(x("airportname").eq(s(to)));


        System.err.println("QUERY: " + query);

        QueryResult result = bucket.query(Query.simple(query));

        String fromAirport = null;
        String toAirport = null;
        for (QueryRow row : result) {
            if (fromAirport == null) {
                fromAirport = row.value().getString("fromAirport");
            }
            if (toAirport == null) {
                toAirport = row.value().getString("toAirport");
            }
        }

        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
        Date parsed = df.parse(leave);
        Calendar instance = Calendar.getInstance(Locale.US);
        instance.setTime(parsed);

        String otherQuery = "SELECT a.name, s.flight, s.utc, r.sourceairport, r.destinationairport, r.equipment FROM `"
            + bucket.name() + "` r UNNEST r.schedule s JOIN `" + bucket.name() + "` a ON KEYS r.airlineid WHERE r.sourceairport='"
            + fromAirport + "' AND r.destinationairport='" + toAirport + "' AND s.day=" + instance.get(Calendar.DAY_OF_MONTH) + " ORDER BY a.name";


        QueryResult otherResult = bucket.query(Query.simple(otherQuery));

        System.err.println("QUERY: " + otherQuery);

        List<Map> content = new ArrayList<Map>();
        if (otherResult.finalSuccess()) {
            for (QueryRow row : otherResult) {
                content.add(row.value().toMap());
            }
        } else {
            System.err.println(result.errors());
        }

        return content;
    }

}
