package com.praio.backend.model;

import lombok.Data;

@Data
public class QualidadeAr {

    private Double iqa;

    private String classificacao;

    private Double pm25;

    private Double ozonio;

    private String estacaoNome;

    private Double distanciaEstacaoKm;

    private String fonte;

    private String observacao;

    private String ultimaAtualizacao;
}
