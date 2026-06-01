package com.praio.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.praio.backend.model.QualidadeAr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Service
public class CetesbService {

    private static final Logger log = LoggerFactory.getLogger(CetesbService.class);

    private static final String URL_AQICN = "https://api.waqi.info/feed/%s/?token=%s";

    @Value("${api.aqicn.token}")
    private String token;

    @Value("${api.aqicn.estacao:santos}")
    private String estacao;

    @Value("${api.aqicn.distancia-km:30}")
    private double distanciaKm;

    private final RestClient restClient;

    public CetesbService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public QualidadeAr buscarQualidadeAr(double latitude, double longitude) {
        try {
            String url = String.format(URL_AQICN, estacao, token);
            log.debug("Consultando AQICN: {}", url.replace(token, "***"));

            JsonNode root = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null || !"ok".equals(textoOuNull(root, "status"))) {
                log.warn("AQICN retornou status inválido para estação '{}'", estacao);
                return null;
            }

            JsonNode data = root.get("data");
            if (data == null || data.isNull()) {
                log.warn("AQICN sem campo 'data' na resposta");
                return null;
            }

            return mapear(data);

        } catch (Exception e) {
            log.error("Falha ao buscar qualidade do ar via AQICN: {}", e.getMessage());
            return null;
        }
    }

    private QualidadeAr mapear(JsonNode data) {
        JsonNode iqaiNode = data.get("iaqi");

        Double aqi    = doubleOuNull(data, "aqi");
        Double pm25   = iqaiNode != null ? doubleDeIaqi(iqaiNode, "pm25") : null;
        Double o3     = iqaiNode != null ? doubleDeIaqi(iqaiNode, "o3")   : null;

        String nomeEstacao = estacao;
        JsonNode cityNode = data.get("city");
        if (cityNode != null && !cityNode.isNull()) {
            String nome = textoOuNull(cityNode, "name");
            if (nome != null) nomeEstacao = nome;
        }

        String classificacao = classificarAqi(aqi);
        String observacao    = observacaoAqi(aqi);

        QualidadeAr qa = new QualidadeAr();
        qa.setIqa(aqi);
        qa.setClassificacao(classificacao);
        qa.setPm25(pm25);
        qa.setOzonio(o3);
        qa.setEstacaoNome(nomeEstacao);
        qa.setDistanciaEstacaoKm(distanciaKm);
        qa.setFonte("CETESB via AQICN");
        qa.setObservacao(observacao);
        qa.setUltimaAtualizacao(Instant.now().toString());

        log.info("Qualidade do ar — Estação: {} · AQI: {} · {}", nomeEstacao, aqi, classificacao);
        return qa;
    }

    private String classificarAqi(Double aqi) {
        if (aqi == null) return "SEM_DADOS";
        if (aqi <= 40)  return "BOA";
        if (aqi <= 80)  return "MODERADA";
        if (aqi <= 120) return "RUIM";
        if (aqi <= 200) return "MUITO_RUIM";
        return "PESSIMA";
    }

    private String observacaoAqi(Double aqi) {
        if (aqi == null) return null;
        if (aqi <= 40)  return "Ideal para atividades ao ar livre.";
        if (aqi <= 80)  return "Aceitável. Pessoas sensíveis devem reduzir esforço prolongado ao ar livre.";
        if (aqi <= 120) return "Grupos vulneráveis devem evitar atividades ao ar livre prolongadas.";
        if (aqi <= 200) return "Todos devem reduzir atividades prolongadas ao ar livre.";
        return "Evite qualquer atividade ao ar livre.";
    }

    private String textoOuNull(JsonNode node, String campo) {
        JsonNode v = node.get(campo);
        if (v == null || v.isNull()) return null;
        String s = v.asText().trim();
        return s.isEmpty() ? null : s;
    }

    private Double doubleOuNull(JsonNode node, String campo) {
        JsonNode v = node.get(campo);
        if (v == null || v.isNull() || !v.isNumber()) return null;
        return v.asDouble();
    }

    private Double doubleDeIaqi(JsonNode iaqi, String poluente) {
        JsonNode pol = iaqi.get(poluente);
        if (pol == null || pol.isNull()) return null;
        JsonNode v = pol.get("v");
        if (v == null || v.isNull() || !v.isNumber()) return null;
        return v.asDouble();
    }
}
