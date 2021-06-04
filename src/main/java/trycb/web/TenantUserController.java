package trycb.web;

import java.util.Map;

import com.couchbase.client.core.msg.kv.DurabilityLevel;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.json.JsonObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import trycb.model.Error;
import trycb.model.IValue;
import trycb.model.Result;
import trycb.service.TenantUser;
import trycb.service.TokenService;

@RestController
@RequestMapping("/api/tenants")
public class TenantUserController {

    private final Bucket bucket;
    private final TenantUser tenantUserService;
    private final TokenService jwtService;

    @Value("${storage.expiry:0}")
    private int expiry;

    @Autowired
    public TenantUserController(Bucket bucket, TenantUser tenantUserService, TokenService jwtService) {
        this.bucket = bucket;
        this.tenantUserService = tenantUserService;
        this.jwtService = jwtService;
    }

    @RequestMapping(value = "/{tenant}/user/login", method = RequestMethod.POST)
    public ResponseEntity<? extends IValue> login(@PathVariable("tenant") String tenant,
            @RequestBody Map<String, String> loginInfo) {
        String user = loginInfo.get("user");
        String password = loginInfo.get("password");
        if (user == null || password == null) {
            return ResponseEntity.badRequest().body(new Error("User or password missing, or malformed request"));
        }

        try {
            return ResponseEntity.ok(tenantUserService.login(bucket, tenant, user, password));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Error(e.getMessage()));
        }
    }

    @RequestMapping(value = "/{tenant}/user/signup", method = RequestMethod.POST)
    public ResponseEntity<? extends IValue> createLogin(@PathVariable("tenant") String tenant,
            @RequestBody String json) {
        JsonObject jsonData = JsonObject.fromJson(json);
        try {
            Result<Map<String, Object>> result = tenantUserService.createLogin(bucket, tenant,
                    jsonData.getString("user"), jsonData.getString("password"), DurabilityLevel.values()[expiry]);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (AuthenticationServiceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Error(e.getMessage()));
        }
    }

    @RequestMapping(value = "/{tenant}/user/{username}/flights", method = RequestMethod.PUT)
    public ResponseEntity<? extends IValue> book(@PathVariable("tenant") String tenant,
            @PathVariable("username") String username, @RequestBody String json,
            @RequestHeader("Authorization") String authentication) {
        if (authentication == null || !authentication.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Error("Bearer Authentication must be used"));
        }
        JsonObject jsonData = JsonObject.fromJson(json);
        try {
            jwtService.verifyAuthenticationHeader(authentication, username);
            Result<Map<String, Object>> result = tenantUserService.registerFlightForUser(bucket, tenant, username,
                    jsonData.getArray("flights"));
            return ResponseEntity.ok().body(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Error("Forbidden, you can't book for this user"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Error(e.getMessage()));
        }
    }

    @RequestMapping(value = "/{tenant}/user/{username}/flights", method = RequestMethod.GET)
    public Object booked(@PathVariable("tenant") String tenant, @PathVariable("username") String username,
            @RequestHeader("Authorization") String authentication) {
        if (authentication == null || !authentication.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Error("Bearer Authentication must be used"));
        }

        try {
            jwtService.verifyAuthenticationHeader(authentication, username);
            return ResponseEntity.ok(tenantUserService.getFlightsForUser(bucket, tenant, username));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Error("Forbidden, you don't have access to this cart"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Error("Forbidden, you don't have access to this cart"));
        }
    }

}
