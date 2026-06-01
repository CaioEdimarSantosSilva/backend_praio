package com.praio.backend.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.praio.backend.model.DadosClimaticos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

@Service
public class OpenMeteoService {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoService.class);

    private static final String VARIAVEIS_ATUAIS =
            "temperature_2m,precipitation,wind_speed_10m,wind_direction_10m," +
            "uv_index,cloud_cover,weather_code";

    @Value("${api.open-meteo.clima.url}")
    private String baseUrl;

    private final RestClient restClient;

    public OpenMeteoService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public DadosClimaticos buscarDadosClimaticos(double latitude, double longitude) {
        try {
            String uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("latitude", latitude)
                    .queryParam("longitude", longitude)
                    .queryParam("current", VARIAVEIS_ATUAIS)
                    .queryParam("timezone", "America/Sao_Paulo")
                    .queryParam("wind_speed_unit", "kmh")
                    .build()
                    .toUriString();

            RespostaOpenMeteo resp = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(RespostaOpenMeteo.class);

            if (resp == null || resp.current() == null) {
                log.warn("Open-Meteo retornou resposta vazia para lat={}, lon={}", latitude, longitude);
                return null;
            }

            return mapear(resp.current());
        } catch (Exception e) {
            log.error("Erro ao buscar dados climáticos Open-Meteo (lat={}, lon={}): {}",
                    latitude, longitude, e.getMessage());
            return null;
        }
    }

    private DadosClimaticos mapear(Current c) {
        DadosClimaticos dados = new DadosClimaticos();
        dados.setTemperaturaAr(c.temperaturaAr());
        dados.setPrecipitacao(c.precipitacao());
        dados.setVelocidadeVento(c.velocidadeVento());
        dados.setDirecaoVento(c.direcaoVento());
        dados.setIndiceUv(c.indiceUv());
        dados.setCoberturaNuvens(c.coberturaNuvens());
        dados.setCodigoClima(c.codigoClima());
        dados.setUltimaAtualizacao(Instant.now().toString());
        return dados;
    }

    private record RespostaOpenMeteo(Current current) {}

    private record Current(
            @JsonProperty("temperature_2m")    Double temperaturaAr,
            @JsonProperty("precipitation")     Double precipitacao,
            @JsonProperty("wind_speed_10m")    Double velocidadeVento,
            @JsonProperty("wind_direction_10m") Double direcaoVento,
            @JsonProperty("uv_index")          Double indiceUv,
            @JsonProperty("cloud_cover")       Integer coberturaNuvens,
            @JsonProperty("weather_code")      Integer codigoClima
    ) {}
}
