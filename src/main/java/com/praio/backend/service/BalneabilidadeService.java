package com.praio.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.praio.backend.model.Balneabilidade;
import com.praio.backend.model.Beach;
import com.praio.backend.repository.BeachRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.Normalizer;
import java.util.Map;

@Service
public class BalneabilidadeService {

    private static final Logger log = LoggerFactory.getLogger(BalneabilidadeService.class);

    private static final Map<String, String> NOME_CETESB_PARA_INTERNO = Map.ofEntries(
            Map.entry("CANTO DO FORTE",    "Canto do Forte"),
            Map.entry("BOQUEIRAO",         "Boqueirão"),
            Map.entry("GUILHERMINA",       "Guilhermina"),
            Map.entry("AVIACAO",           "Aviação"),
            Map.entry("VILA TUPI",         "Tupi"),
            Map.entry("TUPI",              "Tupi"),
            Map.entry("OCIAN",             "Ocian"),
            Map.entry("VILA MIRIM",        "Mirim"),
            Map.entry("MIRIM",             "Mirim"),
            Map.entry("MARACANA",          "Maracanã"),
            Map.entry("VILA CAICARA",      "Caiçara"),
            Map.entry("CAICARA",           "Caiçara"),
            Map.entry("REAL",              "Real"),
            Map.entry("BALNEARIO FLORIDA", "Flórida"),
            Map.entry("FLORIDA",           "Flórida"),
            Map.entry("JARDIM SOLEMAR",    "Solemar"),
            Map.entry("SOLEMAR",           "Solemar")
    );

    @Value("${api.cetesb.arcgis.url}")
    private String arcgisBaseUrl;

    private final BeachRepository beachRepository;
    private final RestClient restClient;

    public BalneabilidadeService(BeachRepository beachRepository, RestClient.Builder builder) {
        this.beachRepository = beachRepository;
        this.restClient      = builder.build();
    }

    @Scheduled(cron = "0 0 9 * * THU", zone = "America/Sao_Paulo")
    public int atualizarBalneabilidade() {
        log.info("Iniciando atualização de balneabilidade via CETESB ArcGIS...");
        try {

            URI uri = UriComponentsBuilder
                    .fromHttpUrl(arcgisBaseUrl + "/query")
                    .queryParam("where", "municipio='PRAIA GRANDE'")
                    .queryParam("outFields", "*")
                    .queryParam("f", "json")
                    .build()
                    .toUri();

            log.info("Chamando CETESB ArcGIS: {}", uri);

            JsonNode root = restClient.get()
                    .uri(uri)
                    .header("User-Agent", "Mozilla/5.0 (compatible; Praio-Backend/1.0)")
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null || !root.has("features") || root.get("features").isEmpty()) {
                log.warn("ArcGIS CETESB retornou resposta vazia ou sem 'features'. Root: {}",
                        root != null ? root.fieldNames() : "null");
                return 0;
            }

            JsonNode features = root.get("features");
            log.info("CETESB ArcGIS retornou {} registros", features.size());

            int atualizadas = 0;
            for (JsonNode feature : features) {
                JsonNode attrs = feature.get("attributes");
                if (attrs == null || attrs.isNull()) continue;

                String nomeCetesb        = textoOuNull(attrs, "praia");
                String classificacaoTexto = textoOuNull(attrs, "classificacao_texto");
                JsonNode qualidadeNode   = attrs.get("qualidade");

                if (nomeCetesb == null) {
                    log.debug("Feature sem campo 'praia': {}", attrs);
                    continue;
                }

                Balneabilidade balneabilidade;
                if (classificacaoTexto != null) {
                    balneabilidade = parsearClassificacao(classificacaoTexto);
                } else if (qualidadeNode != null && !qualidadeNode.isNull()) {
                    balneabilidade = qualidadeNode.asInt() == 1
                            ? Balneabilidade.PROPRIA
                            : Balneabilidade.IMPROPRIA;
                } else {
                    balneabilidade = Balneabilidade.SEM_DADOS;
                }

                String nomeInterno = resolverNome(nomeCetesb);
                if (nomeInterno == null) {
                    log.debug("Praia CETESB não mapeada: '{}'", nomeCetesb);
                    continue;
                }

                var praiaOpt = beachRepository.findByNome(nomeInterno);
                if (praiaOpt.isPresent()) {
                    Beach praia = praiaOpt.get();
                    praia.setBalneabilidade(balneabilidade);
                    beachRepository.save(praia);
                    atualizadas++;
                    log.info("✓ {} → {} (CETESB: '{}')", nomeInterno, balneabilidade, classificacaoTexto);
                } else {
                    log.debug("Praia '{}' não encontrada no MongoDB", nomeInterno);
                }
            }

            log.info("Atualização concluída: {}/12 praias atualizadas.", atualizadas);
            return atualizadas;

        } catch (Exception e) {
            log.error("Falha ao buscar balneabilidade do ArcGIS CETESB: {}", e.getMessage(), e);
            return 0;
        }
    }

    private String textoOuNull(JsonNode node, String campo) {
        JsonNode valor = node.get(campo);
        if (valor == null || valor.isNull()) return null;
        String texto = valor.asText().trim();
        return texto.isEmpty() ? null : texto;
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .trim();
    }

    private String resolverNome(String nomeCetesb) {
        String normalizado = normalizar(nomeCetesb);

        String resultado = NOME_CETESB_PARA_INTERNO.get(normalizado);
        if (resultado != null) return resultado;

        for (Map.Entry<String, String> entry : NOME_CETESB_PARA_INTERNO.entrySet()) {
            if (normalizado.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Balneabilidade parsearClassificacao(String classificacao) {
        String normalizado = normalizar(classificacao);
        if (normalizado.contains("EXCELENTE")) {
            return Balneabilidade.EXCELENTE;
        }
        if (normalizado.contains("MUITO BOA") || normalizado.contains("MUITO_BOA")) {
            return Balneabilidade.MUITO_BOA;
        }
        if (normalizado.contains("SATISFATORIA")) {
            return Balneabilidade.SATISFATORIA;
        }
        if (normalizado.contains("IMPROPRIA") || normalizado.contains("IMPR")) {
            return Balneabilidade.IMPROPRIA;
        }
        if (normalizado.contains("PROPRIA")) {
            return Balneabilidade.PROPRIA;
        }
        return Balneabilidade.SEM_DADOS;
    }
}
