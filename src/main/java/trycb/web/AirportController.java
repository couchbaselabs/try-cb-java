package trycb.web;

import com.couchbase.client.java.Bucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trycb.service.Airport;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/airport")
public class AirportController {

    private final Bucket bucket;

    @Autowired
    public AirportController(Bucket bucket) {
        this.bucket = bucket;
    }

    @RequestMapping("/findAll")
    public List<Map<String, Object>> airports(@RequestParam String search) {
        return Airport.findAll(bucket, search);
    }

}
