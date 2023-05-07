package com.nefentus.api.security;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    public String generateToken(String userEmail, boolean longToken, CustomUserDetails user) {
        Instant now = Instant.now();
        Instant expiration;
        if (!longToken) {
            expiration = now.plus(1, ChronoUnit.DAYS);
        } else {
            expiration = now.plus(30, ChronoUnit.DAYS);
        }
        return Jwts.builder()
                .setClaims(generatePayload(user))
                .setSubject(userEmail)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String generateToken(Authentication authentication, boolean longToken) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return generateToken(user.getEmail(), longToken, user);
    }

    public String getUserMailFromToken(String token) {
        Claims claims = Jwts
                .parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }


    private Map<String, Object> generatePayload(CustomUserDetails userDetail) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("userId", userDetail.getId());
        payload.put("isEnabled", userDetail.isEnabled());
        return payload;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty.");
        }

        return false;
    }

}