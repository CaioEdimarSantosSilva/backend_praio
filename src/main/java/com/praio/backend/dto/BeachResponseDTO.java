package com.praio.backend.dto;

import com.praio.backend.model.Balneabilidade;
import com.praio.backend.model.DadosClimaticos;
import com.praio.backend.model.DadosMaritimos;
import com.praio.backend.model.QualidadeAr;

public record BeachResponseDTO(
        String id,
        String nome,
        String imagem,
        double latitude,
        double longitude,
        Balneabilidade balneabilidade,
        DadosClimaticos dadosClimaticos,
        DadosMaritimos dadosMaritimos,
        QualidadeAr qualidadeAr,
        double score,
        Double avaliacaoMedia,
        long avaliacaoTotal
) {}
