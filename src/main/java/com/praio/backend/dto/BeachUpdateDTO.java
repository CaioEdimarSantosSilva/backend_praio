package com.praio.backend.dto;

public record BeachUpdateDTO(
        String nome,
        Double latitude,
        Double longitude,
        String foto
) {}
