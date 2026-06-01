package com.praio.backend.dto.auth;

import java.util.Set;

public record LoginResponseDTO(
        String token,
        String id,
        String nome,
        String email,
        Set<String> roles
) {}
