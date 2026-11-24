package com.kraiven.taskmanager.security;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.Date;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


/** Creates, parses, and validates signed JWT access tokens. */
@Component 
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.token.lifespan}")
    private Long tokenLifeSpan;

    /** Builds the signing key from the configured secret. */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

/** Creates a token whose subject is the authenticated username. */
public String generateToken(UserDetails userdetails) {
    Map<String, Object> claims = new HashMap<>();
    return createToken(claims, userdetails.getUsername());
}

private String createToken(Map<String, Object> claims, String username) {
    return Jwts.builder().claims(claims)
            .subject(username)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + tokenLifeSpan))
            .signWith(getSigningKey())
            .compact();
}

private Claims extractAllClaims(String token) {
    return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
}

private <T> T extractClaim(String token, Function<Claims, T>claimResolver){
    final Claims claims = extractAllClaims(token);
    return claimResolver.apply(claims);
    
}

public String extractUsernameFromToken(String token) {
    return extractClaim(token, Claims::getSubject);

}

private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);

}

private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());

}

/** Accepts a token only when its username matches and its expiry is still valid. */
public boolean validateToken(String token, UserDetails userDetails) {
    final String username = extractUsernameFromToken(token);
    return username.equals(userDetails.getUsername()) && !isTokenExpired(token);

}





}

