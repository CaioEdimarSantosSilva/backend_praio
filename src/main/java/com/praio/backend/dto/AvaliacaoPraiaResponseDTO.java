package com.praio.backend.dto;

import java.time.Instant;
import java.util.List;

public record AvaliacaoPraiaResponseDTO(
        String id,
        String praiaId,
        String praiaNome,
        String usuarioId,
        String usuarioNome,
        String usuarioFoto,
        String mensagem,
        Integer nota,
        List<String> imagens,
        Instant criadaEm,
        boolean ativo
) {
}
