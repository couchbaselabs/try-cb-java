package trycb.web;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trycb.model.Error;
import trycb.model.IValue;
import trycb.service.FlightPath;

@RestController
@RequestMapping("/api/flightPaths")
public class FlightPathController {

    private final Cluster cluster;
    private final Bucket bucket;

    @Autowired
    public FlightPathController(Cluster cluster, Bucket bucket) {
        this.cluster = cluster;
        this.bucket = bucket;
    }


    @RequestMapping("/{from}/{to}")
    public ResponseEntity<? extends IValue> all(@PathVariable("from") String from, @PathVariable("to") String to,
            @RequestParam String leave) {
        try {
            Calendar calendar = Calendar.getInstance(Locale.US);
            calendar.setTime(DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).parse(leave));
            return ResponseEntity.ok(FlightPath.findAll(cluster, bucket, from, to, calendar));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Error(e.getMessage()));
        }
    }

}
