package trycb;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class Application implements Filter {

    // ======
    // Config Options
    // ======

    @Value("${cb.hostname}")
    private String hostname;

    @Value("${cb.bucket}")
    private String bucket;

    @Value("${cb.password}")
    private String password;

    // ======
    // Bean Definitions
    // ======

    @Bean
    public Cluster cluster() {
        return CouchbaseCluster.create(hostname);
    }

    @Bean
    public Bucket bucket() {
        return cluster().openBucket(bucket, password);
    }

    // ======
    // HTTP Routes
    // ======

    @RequestMapping("/airport/findAll")
    public List<Map<String, Object>> airports(
        @RequestParam("search") String query
    ) {
        return Database.findAllAirports(bucket(), query);
    }

    @RequestMapping("/flightPath/findAll")
    public List<Map<String, Object>> all(
        @RequestParam("from") String from,
        @RequestParam("to") String to,
        @RequestParam("leave") String leave
    ) throws Exception {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.setTime(DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).parse(leave));
        return Database.findAllFlightPaths(bucket(), from, to, calendar);
    }

    // ======
    // Main Method
    // ======

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // ======
    // Enable CORS
    // ======

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}

}
