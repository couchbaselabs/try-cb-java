package trycb.flightpath;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trycb.Application;
import java.util.List;
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
    ) {

        Statement query = select(x("faa").as("fromAirport")).from(i(bucket.name())).where(x("airportname").eq(s(from)))
            .union()
            .select(x("faa").as("toAirport")).from(i(bucket.name())).where(x("airportname").eq(s(to)));


        System.err.println("QUERY: " + query);

        QueryResult result = bucket.query(Query.simple(query));

        System.err.println(result.allRows());

        return null;
    }

}
