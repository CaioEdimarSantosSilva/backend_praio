package com.praio.backend.service;

import com.praio.backend.model.Balneabilidade;
import com.praio.backend.model.Beach;
import com.praio.backend.model.DadosClimaticos;
import com.praio.backend.model.DadosMaritimos;
import com.praio.backend.model.QualidadeAr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ScoreService {

    private static final Logger log = LoggerFactory.getLogger(ScoreService.class);

    private static final double PESO_BALNEABILIDADE = 0.40;
    private static final double PESO_QUALIDADE_AR   = 0.20;
    private static final double PESO_CLIMA          = 0.25;
    private static final double PESO_MARITIMO       = 0.15;

    private static final double NOTA_NEUTRA = 5.0;

    public double calcularScore(Beach beach) {
        double notaBalneabilidade = calcularNotaBalneabilidade(beach.getBalneabilidade());
        double notaQualidadeAr   = calcularNotaQualidadeAr(beach.getQualidadeAr());
        double notaClima         = calcularNotaClima(beach.getDadosClimaticos());
        double notaMaritima      = calcularNotaMaritima(beach.getDadosMaritimos());

        double score = (notaBalneabilidade * PESO_BALNEABILIDADE)
                     + (notaQualidadeAr   * PESO_QUALIDADE_AR)
                     + (notaClima         * PESO_CLIMA)
                     + (notaMaritima      * PESO_MARITIMO);

        double scoreFinal = Math.round(score * 10.0) / 10.0;

        log.debug("Score calculado para praia {}: balneab={}, ar={}, clima={}, mar={} → total={}",
                beach.getNome(), notaBalneabilidade, notaQualidadeAr,
                notaClima, notaMaritima, scoreFinal);

        return scoreFinal;
    }

    private double calcularNotaBalneabilidade(Balneabilidade balneabilidade) {
        if (balneabilidade == null) return NOTA_NEUTRA;
        return switch (balneabilidade) {
            case EXCELENTE   -> 10.0;
            case MUITO_BOA   -> 9.0;
            case SATISFATORIA -> 7.0;
            case PROPRIA     -> 8.0;
            case IMPROPRIA   -> 0.0;
            case SEM_DADOS   -> NOTA_NEUTRA;
        };
    }

    private double calcularNotaQualidadeAr(QualidadeAr qa) {
        if (qa == null || qa.getClassificacao() == null) return NOTA_NEUTRA;
        return switch (qa.getClassificacao()) {
            case "BOA"       -> 10.0;
            case "MODERADA"  -> 7.0;
            case "RUIM"      -> 4.0;
            case "MUITO_RUIM"-> 2.0;
            case "PESSIMA"   -> 0.0;
            default          -> NOTA_NEUTRA;
        };
    }

    private double calcularNotaClima(DadosClimaticos dados) {
        if (dados == null) return NOTA_NEUTRA;

        double nota = 0.0;
        int fatores = 0;

        if (dados.getTemperaturaAr() != null) {
            double temp = dados.getTemperaturaAr();
            double notaTemp;
            if (temp >= 25 && temp <= 32)       notaTemp = 10.0;
            else if (temp >= 22 && temp < 25)   notaTemp = 8.0;
            else if (temp > 32 && temp <= 36)   notaTemp = 7.0;
            else if (temp >= 18 && temp < 22)   notaTemp = 5.0;
            else if (temp > 36)                 notaTemp = 4.0;
            else                                notaTemp = 3.0;
            nota += notaTemp;
            fatores++;
        }

        if (dados.getPrecipitacao() != null) {
            double chuva = dados.getPrecipitacao();
            double notaChuva;
            if (chuva == 0)          notaChuva = 10.0;
            else if (chuva < 1)      notaChuva = 8.0;
            else if (chuva < 5)      notaChuva = 5.0;
            else if (chuva < 20)     notaChuva = 3.0;
            else                     notaChuva = 1.0;
            nota += notaChuva;
            fatores++;
        }

        if (dados.getIndiceUv() != null) {
            double uv = dados.getIndiceUv();
            double notaUv;
            if (uv <= 2)       notaUv = 10.0;
            else if (uv <= 5)  notaUv = 8.0;
            else if (uv <= 7)  notaUv = 6.0;
            else if (uv <= 10) notaUv = 4.0;
            else               notaUv = 2.0;
            nota += notaUv;
            fatores++;
        }

        return fatores > 0 ? nota / fatores : NOTA_NEUTRA;
    }

    private double calcularNotaMaritima(DadosMaritimos dados) {
        if (dados == null || dados.getAlturaOndas() == null) return NOTA_NEUTRA;

        double altura = dados.getAlturaOndas();
        if (altura <= 0.5)       return 10.0;
        else if (altura <= 1.0)  return 8.0;
        else if (altura <= 1.5)  return 6.0;
        else if (altura <= 2.0)  return 4.0;
        else if (altura <= 2.5)  return 2.0;
        else                     return 1.0;
    }
}
