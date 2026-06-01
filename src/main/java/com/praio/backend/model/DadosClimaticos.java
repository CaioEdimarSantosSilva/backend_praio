package com.praio.backend.model;

import lombok.Data;

@Data
public class DadosClimaticos {

    private Double temperaturaAr;

    private Double precipitacao;

    private Double velocidadeVento;

    private Double direcaoVento;

    private Double indiceUv;

    private Integer coberturaNuvens;

    private Integer codigoClima;

    private String ultimaAtualizacao;
}
