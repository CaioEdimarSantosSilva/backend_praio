package com.praio.backend.service;

import com.praio.backend.dto.BeachResponseDTO;
import com.praio.backend.dto.BeachSummaryDTO;
import com.praio.backend.dto.BeachUpdateDTO;
import com.praio.backend.exception.BeachNotFoundException;
import com.praio.backend.model.AvaliacaoPraia;
import com.praio.backend.model.Balneabilidade;
import com.praio.backend.model.Beach;
import com.praio.backend.repository.AvaliacaoPraiaRepository;
import com.praio.backend.repository.BeachRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class BeachService {

    private static final Logger log = LoggerFactory.getLogger(BeachService.class);

    private static final long MAX_IMAGEM_PRAIA_BYTES = 5 * 1024 * 1024;

    @Value("${praio.cache.ttl-minutos:30}")
    private int ttlMinutos;

    @Value("${praio.uploads.dir:uploads}")
    private String uploadsDir;

    private final BeachRepository       beachRepository;
    private final AvaliacaoPraiaRepository avaliacaoPraiaRepository;
    private final OpenMeteoService      openMeteoService;
    private final OpenMeteoMarineService openMeteoMarineService;
    private final CetesbService         cetesbService;
    private final ScoreService          scoreService;

    public BeachService(
            BeachRepository beachRepository,
            AvaliacaoPraiaRepository avaliacaoPraiaRepository,
            OpenMeteoService openMeteoService,
            OpenMeteoMarineService openMeteoMarineService,
            CetesbService cetesbService,
            ScoreService scoreService) {
        this.beachRepository        = beachRepository;
        this.avaliacaoPraiaRepository = avaliacaoPraiaRepository;
        this.openMeteoService       = openMeteoService;
        this.openMeteoMarineService = openMeteoMarineService;
        this.cetesbService          = cetesbService;
        this.scoreService           = scoreService;
    }

    public List<BeachSummaryDTO> listarTodas() {
        log.debug("Listando todas as praias");
        List<Beach> praias = beachRepository.findAll();

        praias.forEach(praia -> {
            if (dadosDesatualizados(praia)) {
                log.info("Atualizando dados externos da praia '{}' durante listagem", praia.getNome());
                enriquecerDadosExternos(praia);
            }
        });

        return praias.stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    public List<BeachSummaryDTO> listarPorBalneabilidade(Balneabilidade balneabilidade) {
        log.debug("Listando praias com balneabilidade: {}", balneabilidade);
        List<Beach> praias = beachRepository.findByBalneabilidade(balneabilidade);
        return praias.stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    public List<BeachSummaryDTO> listarMelhoresPraias(double scoreMinimo) {
        log.debug("Listando praias com score >= {}", scoreMinimo);
        List<Beach> praias = beachRepository.findByScoreGreaterThanEqualOrderByScoreDesc(scoreMinimo);
        return praias.stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    public BeachResponseDTO buscarPorId(String id) {
        log.debug("Buscando praia com ID: {}", id);

        Beach beach = beachRepository.findById(id)
                .orElseThrow(() -> new BeachNotFoundException(id));

        if (dadosDesatualizados(beach)) {
            log.info("Atualizando dados externos da praia '{}' (TTL expirado ou dados ausentes)", beach.getNome());
            enriquecerDadosExternos(beach);
        } else {
            log.debug("Dados da praia '{}' ainda dentro do TTL — sem chamada externa", beach.getNome());
        }

        return toResponseDTO(beach);
    }

    private BeachSummaryDTO toSummaryDTO(Beach beach) {
        AvaliacaoResumo resumo = calcularResumoAvaliacoes(beach.getId());

        return new BeachSummaryDTO(
                beach.getId(),
                beach.getNome(),
                beach.getImagem(),
                beach.getLatitude(),
                beach.getLongitude(),
                beach.getBalneabilidade(),
                beach.getDadosClimaticos(),
                beach.getDadosMaritimos(),
                beach.getQualidadeAr(),
                beach.getScore(),
                resumo.media(),
                resumo.total()
        );
    }

    private BeachResponseDTO toResponseDTO(Beach beach) {
        AvaliacaoResumo resumo = calcularResumoAvaliacoes(beach.getId());

        return new BeachResponseDTO(
                beach.getId(),
                beach.getNome(),
                beach.getImagem(),
                beach.getLatitude(),
                beach.getLongitude(),
                beach.getBalneabilidade(),
                beach.getDadosClimaticos(),
                beach.getDadosMaritimos(),
                beach.getQualidadeAr(),
                beach.getScore(),
                resumo.media(),
                resumo.total()
        );
    }

    private AvaliacaoResumo calcularResumoAvaliacoes(String praiaId) {
        List<Integer> notas = avaliacaoPraiaRepository.findByPraiaIdAndAtivoTrueOrderByCriadaEmDesc(praiaId).stream()
                .map(AvaliacaoPraia::getNota)
                .filter(Objects::nonNull)
                .toList();

        if (notas.isEmpty()) {
            return new AvaliacaoResumo(null, 0);
        }

        double media = notas.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        return new AvaliacaoResumo(Math.round(media * 10.0) / 10.0, notas.size());
    }

    private record AvaliacaoResumo(Double media, long total) {}

    public Beach atualizarPraia(String id, BeachUpdateDTO dto) {
        Beach praia = beachRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Praia não encontrada: " + id));

        if (dto.nome() != null && !dto.nome().isBlank()) {
            praia.setNome(dto.nome());
        }
        if (dto.latitude() != null) {
            praia.setLatitude(dto.latitude());
        }
        if (dto.longitude() != null) {
            praia.setLongitude(dto.longitude());
        }
        if (dto.foto() != null && !dto.foto().isBlank()) {
            String imagemPath = salvarImagemPraia(praia.getNome(), praia.getImagem(), dto.foto());
            praia.setImagem(imagemPath);
        }

        beachRepository.save(praia);
        log.info("Praia '{}' atualizada pelo admin", praia.getNome());
        return praia;
    }

    private String salvarImagemPraia(String nomePraia, String imagemAtual, String base64) {
        String ext  = detectarExtensaoImagem(base64);
        String dados = base64.contains(",") ? base64.split(",", 2)[1] : base64;

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dados);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Imagem inválida: formato base64 incorreto.");
        }
        if (bytes.length > MAX_IMAGEM_PRAIA_BYTES) {
            throw new IllegalArgumentException("Imagem muito grande. Tamanho máximo: 5 MB.");
        }

        if (imagemAtual != null) {
            if (imagemAtual.startsWith("/uploads/praias/")) {

                removerImagemPraia("praias/" + imagemAtual.substring("/uploads/praias/".length()));
            } else if (imagemAtual.matches(".*_[0-9a-f]{8}\\.[a-z]+$")) {

                removerImagemPraia("praias/" + imagemAtual);
            }

        }

        String slug    = slugNomePraia(nomePraia);
        String uid     = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String arquivo = slug + "_" + uid + "." + ext;

        Path dir     = Paths.get(uploadsDir, "praias");
        Path destino = dir.resolve(arquivo);

        try {
            Files.createDirectories(dir);
            Files.write(destino, bytes);
            log.info("Imagem da praia salva: {}", destino.toAbsolutePath());
        } catch (IOException e) {
            log.error("Erro ao salvar imagem da praia '{}': {}", nomePraia, e.getMessage());
            throw new RuntimeException("Não foi possível salvar a imagem. Tente novamente.");
        }

        return arquivo;
    }

    private void removerImagemPraia(String relativePath) {
        try {
            Path arquivo = Paths.get(uploadsDir, relativePath);
            Files.deleteIfExists(arquivo);
        } catch (IOException e) {
            log.warn("Não foi possível remover imagem antiga da praia: {}", e.getMessage());
        }
    }

    private String detectarExtensaoImagem(String base64) {
        if (base64.startsWith("data:image/png"))  return "png";
        if (base64.startsWith("data:image/webp")) return "webp";
        return "jpg";
    }

    private String slugNomePraia(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    public Map<String, Object> forcarAtualizacaoClima() {
        List<Beach> praias = beachRepository.findAll();
        int atualizadas = 0;
        List<String> erros = new ArrayList<>();

        for (Beach praia : praias) {
            try {
                var dados = openMeteoService.buscarDadosClimaticos(praia.getLatitude(), praia.getLongitude());
                if (dados != null) {
                    praia.setDadosClimaticos(dados);
                    praia.setScore(scoreService.calcularScore(praia));
                    praia.setUltimaAtualizacaoDados(Instant.now());
                    beachRepository.save(praia);
                    atualizadas++;
                } else {
                    log.warn("Open-Meteo retornou null para '{}'", praia.getNome());
                    erros.add(praia.getNome());
                }
            } catch (Exception e) {
                log.error("Falha ao forçar clima de '{}': {}", praia.getNome(), e.getMessage());
                erros.add(praia.getNome());
            }
        }

        log.info("Atualização forçada de clima: {}/{} praias", atualizadas, praias.size());
        return Map.of("praias_atualizadas", atualizadas, "total", praias.size(),
                      "fonte", "Open-Meteo", "erros", erros);
    }

    public Map<String, Object> forcarAtualizacaoMaritimo() {
        List<Beach> praias = beachRepository.findAll();
        int atualizadas = 0;
        List<String> erros = new ArrayList<>();

        for (Beach praia : praias) {
            try {
                var dados = openMeteoMarineService.buscarDadosMaritimos(praia.getLatitude(), praia.getLongitude());
                if (dados != null) {
                    praia.setDadosMaritimos(dados);
                    praia.setScore(scoreService.calcularScore(praia));
                    praia.setUltimaAtualizacaoDados(Instant.now());
                    beachRepository.save(praia);
                    atualizadas++;
                } else {
                    log.warn("Open-Meteo Marine retornou null para '{}'", praia.getNome());
                    erros.add(praia.getNome());
                }
            } catch (Exception e) {
                log.error("Falha ao forçar dados marítimos de '{}': {}", praia.getNome(), e.getMessage());
                erros.add(praia.getNome());
            }
        }

        log.info("Atualização forçada marítima: {}/{} praias", atualizadas, praias.size());
        return Map.of("praias_atualizadas", atualizadas, "total", praias.size(),
                      "fonte", "Open-Meteo Marine", "erros", erros);
    }

    public Map<String, Object> forcarAtualizacaoAr() {
        List<Beach> praias = beachRepository.findAll();
        int atualizadas = 0;
        List<String> erros = new ArrayList<>();

        for (Beach praia : praias) {
            try {
                var dados = cetesbService.buscarQualidadeAr(praia.getLatitude(), praia.getLongitude());
                if (dados != null) {
                    praia.setQualidadeAr(dados);
                    praia.setScore(scoreService.calcularScore(praia));
                    praia.setUltimaAtualizacaoDados(Instant.now());
                    beachRepository.save(praia);
                    atualizadas++;
                } else {
                    log.warn("AQICN retornou null para '{}'", praia.getNome());
                    erros.add(praia.getNome());
                }
            } catch (Exception e) {
                log.error("Falha ao forçar qualidade do ar de '{}': {}", praia.getNome(), e.getMessage());
                erros.add(praia.getNome());
            }
        }

        log.info("Atualização forçada de ar: {}/{} praias", atualizadas, praias.size());
        return Map.of("praias_atualizadas", atualizadas, "total", praias.size(),
                      "fonte", "CETESB QUALAR", "erros", erros);
    }

    public Map<String, Object> forcarAtualizacaoCompleta() {
        List<Beach> praias = beachRepository.findAll();
        int atualizadas = 0;
        List<String> erros = new ArrayList<>();

        for (Beach praia : praias) {
            List<String> falhas = new ArrayList<>();

            try {
                var dados = openMeteoService.buscarDadosClimaticos(praia.getLatitude(), praia.getLongitude());
                if (dados != null) praia.setDadosClimaticos(dados);
                else { log.warn("Clima null para '{}'", praia.getNome()); falhas.add("clima"); }
            } catch (Exception e) {
                log.error("Falha clima '{}': {}", praia.getNome(), e.getMessage());
                falhas.add("clima");
            }

            try {
                var dados = openMeteoMarineService.buscarDadosMaritimos(praia.getLatitude(), praia.getLongitude());
                if (dados != null) praia.setDadosMaritimos(dados);
                else { log.warn("Marítimo null para '{}'", praia.getNome()); falhas.add("marítimo"); }
            } catch (Exception e) {
                log.error("Falha marítimo '{}': {}", praia.getNome(), e.getMessage());
                falhas.add("marítimo");
            }

            try {
                var dados = cetesbService.buscarQualidadeAr(praia.getLatitude(), praia.getLongitude());
                if (dados != null) praia.setQualidadeAr(dados);
                else { log.warn("Ar null para '{}'", praia.getNome()); falhas.add("ar"); }
            } catch (Exception e) {
                log.error("Falha ar '{}': {}", praia.getNome(), e.getMessage());
                falhas.add("ar");
            }

            try {
                praia.setScore(scoreService.calcularScore(praia));
                praia.setUltimaAtualizacaoDados(Instant.now());
                beachRepository.save(praia);
                if (falhas.isEmpty()) {
                    atualizadas++;
                } else {
                    erros.add(praia.getNome() + " (" + String.join(", ", falhas) + ")");
                }
            } catch (Exception e) {
                log.error("Falha ao salvar '{}': {}", praia.getNome(), e.getMessage());
                erros.add(praia.getNome() + " (erro ao salvar)");
            }
        }

        java.util.Set<String> tiposSet = new java.util.LinkedHashSet<>();
        for (String erro : erros) {
            int ini = erro.indexOf('(');
            int fim = erro.lastIndexOf(')');
            if (ini >= 0 && fim > ini) {
                for (String tipo : erro.substring(ini + 1, fim).split(",\\s*")) {
                    tiposSet.add(tipo.trim());
                }
            }
        }
        List<String> tiposComFalha = new ArrayList<>(tiposSet);

        log.info("Atualização forçada completa: {}/{} praias sem falhas", atualizadas, praias.size());
        return Map.of(
                "praias_atualizadas", atualizadas,
                "total", praias.size(),
                "fonte", "Open-Meteo + Open-Meteo Marine + CETESB QUALAR",
                "erros", erros,
                "tipos_com_falha", tiposComFalha
        );
    }

    private boolean dadosDesatualizados(Beach beach) {
        if (beach.getUltimaAtualizacaoDados() == null) return true;

        if (beach.getDadosClimaticos() == null
                || beach.getDadosMaritimos() == null
                || beach.getQualidadeAr()   == null) return true;
        Instant limite = Instant.now().minus(ttlMinutos, ChronoUnit.MINUTES);
        return beach.getUltimaAtualizacaoDados().isBefore(limite);
    }

    private void enriquecerDadosExternos(Beach beach) {
        double lat = beach.getLatitude();
        double lon = beach.getLongitude();

        try {
            var dadosClimaticos = openMeteoService.buscarDadosClimaticos(lat, lon);
            if (dadosClimaticos != null) {
                beach.setDadosClimaticos(dadosClimaticos);
                log.debug("Clima atualizado para '{}'", beach.getNome());
            }
        } catch (Exception e) {
            log.error("Falha ao buscar clima para '{}': {}", beach.getNome(), e.getMessage());
        }

        try {
            var dadosMaritimos = openMeteoMarineService.buscarDadosMaritimos(lat, lon);
            if (dadosMaritimos != null) {
                beach.setDadosMaritimos(dadosMaritimos);
                log.debug("Dados marítimos atualizados para '{}'", beach.getNome());
            }
        } catch (Exception e) {
            log.error("Falha ao buscar dados marítimos para '{}': {}", beach.getNome(), e.getMessage());
        }

        try {
            var qualidadeAr = cetesbService.buscarQualidadeAr(lat, lon);
            if (qualidadeAr != null) {
                beach.setQualidadeAr(qualidadeAr);
                log.debug("Qualidade do ar atualizada para '{}'", beach.getNome());
            }
        } catch (Exception e) {
            log.error("Falha ao buscar qualidade do ar para '{}': {}", beach.getNome(), e.getMessage());
        }

        beach.setScore(scoreService.calcularScore(beach));
        beach.setUltimaAtualizacaoDados(Instant.now());

        beachRepository.save(beach);
        log.info("Praia '{}' enriquecida e salva. Score={}", beach.getNome(), beach.getScore());
    }
}
