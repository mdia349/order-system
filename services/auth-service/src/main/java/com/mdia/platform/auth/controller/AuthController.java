package com.mdia.platform.auth.controller;

import com.mdia.platform.auth.dto.LoginRequest;
import com.mdia.platform.auth.dto.RegisterRequest;
import com.mdia.platform.auth.dto.TokenResponse;
import com.mdia.platform.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody RegisterRequest request) {
        authService.register(request.email(), request.password());
    }

    @PostMapping("/auth/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        String token = authService.loginAndIssueToken(request.email(), request.password());
        return new TokenResponse(token);
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        var roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();
        return Map.of(
                "subject", auth.getName(),
                "roles", roles
        );
    }
}
