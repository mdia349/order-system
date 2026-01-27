package com.mdia.platform.auth.service;

import com.mdia.platform.auth.user.AppUser;
import com.mdia.platform.auth.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    private final String issuer;
    private final long accessTokenMinutes;

    public AuthService(
            AppUserRepository repo,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.accessTokenMinutes}") long accessTokenMinutes
    ) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this. jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public void register(String emailRaw, String passwordRaw) {
        if (emailRaw == null || passwordRaw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email and password required");
        }

        String email = normalizeEmail(emailRaw);

        if (repo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }

        AppUser user = new AppUser(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(passwordRaw),
                "ROLE_USER"
        );

        repo.save(user);
    }

    public String loginAndIssueToken(String emailRaw, String passwordRaw) {
        if (emailRaw == null || passwordRaw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email and password required");
        }

        String email = normalizeEmail(emailRaw);

        AppUser user = repo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));

        if (!passwordEncoder.matches(passwordRaw, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }

        return issueAccessToken(user);
    }

    private String issueAccessToken(AppUser user) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(exp)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles())
                .build();
        return jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims
                )
        ).getTokenValue();
    }

    private String normalizeEmail(String emailRaw) {
        return emailRaw.trim().toLowerCase();
    }
}
