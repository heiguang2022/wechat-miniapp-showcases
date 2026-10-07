package com.yike.coffee.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessMinutes;

    public JwtService(@Value("${app.auth.jwt-secret}") String secret,
                      @Value("${app.auth.access-minutes}") long accessMinutes) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalStateException("JWT secret 至少需要 32 字节");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMinutes = accessMinutes;
    }

    public String create(AppPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder().subject(principal.userId()).issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(accessMinutes, ChronoUnit.MINUTES)))
            .claim("email", principal.email()).claim("name", principal.displayName())
            .claim("role", principal.role()).claim("merchantId", principal.merchantId())
            .signWith(key).compact();
    }

    public AppPrincipal parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AppPrincipal(c.getSubject(), c.get("email", String.class), c.get("name", String.class),
            c.get("role", String.class), c.get("merchantId", String.class));
    }
}
