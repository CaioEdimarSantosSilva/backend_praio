package com.praio.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AvaliacaoPraiaRequestDTO(
        @NotBlank(message = "mensagem e obrigatoria")
        @Size(max = 800, message = "mensagem deve ter no maximo 800 caracteres")
        String mensagem,

        @NotNull(message = "nota e obrigatoria")
        @Min(value = 1, message = "nota deve ser no minimo 1")
        @Max(value = 5, message = "nota deve ser no maximo 5")
        Integer nota,

        @Size(max = 4, message = "envie no maximo 4 imagens")
        List<@Size(max = 1_500_000, message = "imagem muito grande") String> imagens
) {
}
