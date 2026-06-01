package com.praio.backend.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.praio.backend.model.DadosMaritimos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

@Service
public class OpenMeteoMarineService {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoMarineService.class);

    private static final String VARIAVEIS_ATUAIS =
            "wave_height,wave_period,wave_direction,sea_surface_temperature";

    @Value("${api.open-meteo.marine.url}")
    private String baseUrl;

    private final RestClient restClient;

    public OpenMeteoMarineService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public DadosMaritimos buscarDadosMaritimos(double latitude, double longitude) {
        try {
            String uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("latitude", latitude)
                    .queryParam("longitude", longitude)
                    .queryParam("current", VARIAVEIS_ATUAIS)
                    .queryParam("timezone", "America/Sao_Paulo")
                    .build()
                    .toUriString();

            RespostaMarinha resp = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(RespostaMarinha.class);

            if (resp == null || resp.current() == null) {
                log.warn("Open-Meteo Marine retornou resposta vazia para lat={}, lon={}", latitude, longitude);
                return null;
            }

            return mapear(resp.current());
        } catch (Exception e) {
            log.error("Erro ao buscar dados marítimos Open-Meteo Marine (lat={}, lon={}): {}",
                    latitude, longitude, e.getMessage());
            return null;
        }
    }

    private DadosMaritimos mapear(Current c) {
        DadosMaritimos dados = new DadosMaritimos();
        dados.setAlturaOndas(c.alturaOndas());
        dados.setPeriodoOndas(c.periodoOndas());
        dados.setDirecaoOndas(c.direcaoOndas());
        dados.setTemperaturaAgua(c.temperaturaAgua());
        dados.setUltimaAtualizacao(Instant.now().toString());
        return dados;
    }

    private record RespostaMarinha(Current current) {}

    private record Current(
            @JsonProperty("wave_height")              Double alturaOndas,
            @JsonProperty("wave_period")              Double periodoOndas,
            @JsonProperty("wave_direction")           Double direcaoOndas,
            @JsonProperty("sea_surface_temperature")  Double temperaturaAgua
    ) {}
}
