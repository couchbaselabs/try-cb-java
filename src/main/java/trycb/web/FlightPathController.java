package trycb.web;

import com.couchbase.client.java.Bucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trycb.service.FlightPath;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/flightPath")
public class FlightPathController {

    private final Bucket bucket;

    @Autowired
    public FlightPathController(Bucket bucket) {
        this.bucket = bucket;
    }


    @RequestMapping("/findAll")
    public List<Map<String, Object>> all(@RequestParam String from, @RequestParam String to, @RequestParam String leave)
        throws Exception {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.setTime(DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).parse(leave));
        return FlightPath.findAll(bucket, from, to, calendar);
    }

}
