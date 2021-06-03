package trycb.web;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import trycb.model.Error;
import trycb.model.IValue;
import trycb.service.Airport;

@RestController
@RequestMapping("/api/airports")
public class AirportController {

    private final Cluster cluster;
    private final Bucket bucket;

    @Autowired
    public AirportController(Cluster cluster, Bucket bucket) {
        this.cluster = cluster;
        this.bucket = bucket;
    }

    @RequestMapping
    public ResponseEntity<? extends IValue> airports(@RequestParam("search") String search) {
        try {
            return ResponseEntity.ok(Airport.findAll(cluster, bucket.name(), search));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Error(e.getMessage()));
        }
    }

}
