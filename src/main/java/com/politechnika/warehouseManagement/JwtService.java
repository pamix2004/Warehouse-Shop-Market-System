package com.politechnika.warehouseManagement;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    @Value("${app.secretKey}")
    private String secretKey;
    /**
     * @brief It makes the token based on userID. It is then maade to expire after given time
     * */
    public String createVerificationToken(int userId) {
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

    public  int handleVerification(String token) throws ExpiredJwtException, JwtException {
        Claims claims;
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Integer.parseInt(claims.getSubject());

    }
}
