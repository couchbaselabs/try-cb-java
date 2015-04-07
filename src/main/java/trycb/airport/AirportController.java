package trycb.airport;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryRow;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.path.AsPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trycb.Application;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;

@RestController
@RequestMapping(Application.URI_PREFIX + "/airport")
public class AirportController {

    private final Bucket bucket;

    @Autowired
    public AirportController(final Bucket bucket) {
        this.bucket = bucket;
    }

    @RequestMapping("findAll")
    public List<Map> all(@RequestParam("search") String query) {
        Statement q;

        AsPath prefix = select("airportname").from(i(bucket.name()));
        if (query.length() == 3) {
            q = prefix.where(x("faa").eq(s(query.toUpperCase())));
        } else if (query.length() == 4 && (query.equals(query.toUpperCase())||query.equals(query.toLowerCase()))) {
            q = prefix.where(x("icao").eq(s(query.toUpperCase())));
        } else {
            q = prefix.where(i("airportname").like(s(query + "%")));
        }

        QueryResult result = bucket.query(Query.simple(q));

        System.err.println("QUERY: " + q);

        List<Map> content = new ArrayList<Map>();
        if (result.finalSuccess()) {
            for (QueryRow row : result) {
                content.add(row.value().toMap());
            }
        } else {
            System.err.println(result.errors());
        }

        return content;
    }
}
