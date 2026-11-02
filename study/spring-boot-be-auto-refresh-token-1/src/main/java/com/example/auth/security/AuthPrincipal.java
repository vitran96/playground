package com.example.auth.security;

import java.util.UUID;

public record AuthPrincipal(String username, UUID sessionId) {}
