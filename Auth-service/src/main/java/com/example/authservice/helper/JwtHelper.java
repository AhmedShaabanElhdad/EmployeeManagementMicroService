package com.example.authservice.helper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtHelper {

    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 15;
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7;
    private static final long SERVICE_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24; // 24 hours

    @Value("${jwt.secret}")
    private String jwtSecret;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String generateAccessToken(
            Map<String, Object> claims,
            UserDetails userDetails
    ) {
        return buildToken(
                claims,
                userDetails.getUsername(),
                ACCESS_TOKEN_EXPIRATION
        );
    }

    public String generateRefreshToken(
            Map<String, Object> claims,
            UserDetails userDetails
    ) {
        return buildToken(
                claims,
                userDetails.getUsername(),
                REFRESH_TOKEN_EXPIRATION
        );
    }

    public String generateInternalServiceToken(String serviceName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "SERVICE");
        claims.put("type", "access");
        claims.put("jti", UUID.randomUUID().toString());
        
        return buildToken(
                claims,
                serviceName,
                SERVICE_TOKEN_EXPIRATION
        );
    }

    private String buildToken(
            Map<String, Object> claims,
            String subject,
            long expiration
    ) {

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(
                        new Date(
                                System.currentTimeMillis() + expiration
                        )
                )
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isAccessTokenValid(
            String token,
            UserDetails userDetails
    ) {

        final String username = extractUsername(token);

        final String tokenType =
                extractClaim(token, claims ->
                        claims.get("type", String.class)
                );

        return Objects.equals(
                username,
                userDetails.getUsername()
        )
                && !isTokenExpired(token)
                && "access".equals(tokenType);
    }

    public boolean isRefreshTokenValid(
            String token,
            UserDetails userDetails
    ) {

        final String username = extractUsername(token);

        final String tokenType =
                extractClaim(token, claims ->
                        claims.get("type", String.class)
                );

        return Objects.equals(
                username,
                userDetails.getUsername()
        )
                && !isTokenExpired(token)
                && "refresh".equals(tokenType);
    }

    public <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    ) {

        final Claims claims = extractAllClaims(token);

        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {

        Date expirationDate =
                extractClaim(token, Claims::getExpiration);

        return expirationDate.before(new Date());
    }

    public Claims validateToken(String token) {

        try {

            return extractAllClaims(token);

        } catch (Exception ex) {

            return null;
        }
    }

    private SecretKey getSigningKey() {

        byte[] keyBytes =
                Decoders.BASE64.decode(jwtSecret);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
