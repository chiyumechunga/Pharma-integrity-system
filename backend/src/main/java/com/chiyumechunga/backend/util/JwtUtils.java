package com.chiyumechunga.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtils {

    // UPDATE: In 0.12+, Keys.secretKeyFor is replaced. We now use Jwts.SIG.
    private final SecretKey key = Jwts.SIG.HS256.key().build();

    // HARDCODED DEFAULT: To prevent unit test failure when Spring isn't loaded
    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationMs = 86400000L;

    public String generateToken(String email, String role, String userId) {
        return Jwts.builder()
                .subject(email) // UPDATE: setSubject() is now subject()
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(new Date()) // UPDATE: setIssuedAt() is now issuedAt()
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // UPDATE: setExpiration() is now expiration()
                .signWith(key)
                .compact();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            // UPDATE: parserBuilder() is gone. Replaced by parser().verifyWith()
            Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException e) {
            log.error("Invalid JWT Token: {}", e.getMessage());
        }
        return false;
    }

    public String getUserNameFromJwtToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        // UPDATE: Extracting claims also uses the new parser syntax
        final Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload(); // UPDATE: getBody() is now getPayload()
        return claimsResolver.apply(claims);
    }
}