package com.mdia.platform.auth.dto;

public record LoginRequest(
        String email,
        String password
) {}
