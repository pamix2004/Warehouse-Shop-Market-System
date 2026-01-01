package com.warehouseManagement.demo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@RestController
@RequestMapping("/jwt")
public class JwtServiceRestController {
    @Value("${app.secretKey}")
    private String secretKey;
    /**
     * It makes the token based on userID. It is then maade to expire after given time
     * */
    @GetMapping("/createJWTToken")
    public String createVerificationToken(@RequestParam("id")int userId) {
        Instant now = Instant.now();
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);    // 256-bit Base64 string
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(15))))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * It verifies the given token and derives userID. If token is invalid it returns -1 and status code of either 401 or 504.
     * */
    @PostMapping("/verifyJWTToken")
    public ResponseEntity<Integer> verifyJWTToken(@RequestParam String token){
        try{
            Claims claims;
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);

            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            int userID = Integer.parseInt(claims.getSubject());
            return ResponseEntity.ok(userID);
        }
        //If token is expired it returns -1 and STATUS CODE of 504
        catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(-1);
        }
        //Invalid token, returns -1 and 401 http status code
        catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(-1);
        }


    }

}
