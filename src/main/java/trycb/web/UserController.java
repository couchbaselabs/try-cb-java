package trycb.web;


import java.util.List;
import java.util.Map;

import com.couchbase.client.core.msg.kv.DurabilityLevel;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import trycb.service.TokenService;
import trycb.service.User;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final Scope scope;
    private final User userService;
    private final TokenService jwtService;

    @Value("${storage.expiry:0}")
    private int expiry;

    @Autowired
    public UserController(@Qualifier("clientOrgScope") Scope scope, User userService, TokenService jwtService) {
        this.scope = scope;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @RequestMapping(value="/login", method= RequestMethod.POST)
    public ResponseEntity<? extends IValue> login(@RequestBody Map<String, String> loginInfo) {
        String user = loginInfo.get("user");
        String password = loginInfo.get("password");
        if (user == null || password == null) {
            return ResponseEntity.badRequest().body(new Error("User or password missing, or malformed request"));
        }

        try {
            Map<String, Object> data = userService.login(scope, user, password);
            return ResponseEntity.ok(Result.of(data));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new Error(e.getMessage()));
        }
    }

    @RequestMapping(value="/signup", method=RequestMethod.POST)
    public ResponseEntity<? extends IValue> createLogin(@RequestBody String json) {
        JsonObject jsonData = JsonObject.fromJson(json);
        try {
            Result<Map<String, Object>> result = userService.createLogin(scope, jsonData.getString("user"),
                    jsonData.getString("password"), DurabilityLevel.values()[expiry]);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(result);
        } catch (AuthenticationServiceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new Error(e.getMessage()));
        }
    }

    @RequestMapping(value="/{username}/flights", method=RequestMethod.POST)
    public ResponseEntity<? extends IValue> book(@PathVariable("username") String username, @RequestBody String json,
            @RequestHeader("Authorization") String authentication) {
        if (authentication == null || !authentication.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Error("Bearer Authentication must be used"));
        }
        JsonObject jsonData = JsonObject.fromJson(json);
        try {
            jwtService.verifyAuthenticationHeader(authentication, username);
            Result<Map<String, Object>> result = userService.registerFlightForUser(scope, username, jsonData.getArray("flights"));
            return ResponseEntity.accepted()
                    .body(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Error("Forbidden, you can't book for this user"));
        } catch(IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Error(e.getMessage()));
        }
    }

    @RequestMapping(value="/{username}/flights", method=RequestMethod.GET)
    public Object booked(@PathVariable("username") String username, @RequestHeader("Authentication") String authentication) {
        if (authentication == null || !authentication.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Error("Bearer Authentication must be used"));
        }

        try {
            jwtService.verifyAuthenticationHeader(authentication, username);

            List<Map<String, Object>> flights = userService.getFlightsForUser(scope, username);
            return ResponseEntity.ok(Result.of(flights));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Error("Forbidden, you don't have access to this cart"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Error("Forbidden, you don't have access to this cart"));
        }
    }

}
