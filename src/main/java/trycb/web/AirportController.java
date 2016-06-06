package trycb.web;

import java.util.List;
import java.util.Map;

import com.couchbase.client.java.Bucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trycb.model.Error;
import trycb.model.IValue;
import trycb.model.Result;
import trycb.service.Airport;

@RestController
@RequestMapping("/api/airport")
public class AirportController {

    private final Bucket bucket;

    @Autowired
    public AirportController(Bucket bucket) {
        this.bucket = bucket;
    }

    @RequestMapping("/findAll")
    public ResponseEntity<? extends IValue> airports(@RequestParam String search) {
        try {
            Result<List<Map<String, Object>>> result = Airport.findAll(bucket, search);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Error(e.getMessage()));
        }
    }

}
