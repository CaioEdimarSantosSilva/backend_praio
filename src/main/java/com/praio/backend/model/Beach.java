package com.praio.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "praias")
public class Beach {

    @Id
    private String id;

    private String nome;

    private String imagem;

    private double latitude;

    private double longitude;

    private Balneabilidade balneabilidade;

    private DadosClimaticos dadosClimaticos;

    private DadosMaritimos dadosMaritimos;

    private QualidadeAr qualidadeAr;

    private double score;

    private Instant ultimaAtualizacaoDados;
}
