package com.intelimart.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
// Removed: import org.springframework.security.core.userdetails.UserDetails; // No longer needed
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap; // Still needed for createToken if it existed, but we're removing it
import java.util.Map;     // Still needed for createToken if it existed, but we're removing it
import java.util.function.Function;

@Component // Marks this as a Spring component to be managed by the IoC container
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration}")
    private long EXPIRATION_TIME; // This is still used in isTokenExpired indirectly

    // Retrieve signing key
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Extract a single claim from the token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extract all claims (payload) from the token
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .parseClaimsJws(token)
                .getBody();
    }

    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract expiration date from token
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Check if the token is expired
    public Boolean isTokenExpired(String token) { // Changed to public for direct use in filter
        return extractExpiration(token).before(new Date());
    }

    // Removed: validateToken(String token, UserDetails userDetails)
    // Removed: generateToken(UserDetails userDetails)
    // Removed: createToken(Map<String, Object> claims, String subject)
}