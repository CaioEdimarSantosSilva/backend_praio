package com.praio.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record UsuarioResponseDTO(
        String id,
        String nome,
        String email,
        Set<String> roles,
        boolean ativo,
        List<String> favoritosIds,
        Instant dataCriacao,

        String foto
) {}
