package com.praio.backend;

import com.praio.backend.model.Beach;
import com.praio.backend.model.Balneabilidade;
import com.praio.backend.repository.BeachRepository;
import com.praio.backend.service.BalneabilidadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class BackendApplication {

	private static final Logger log = LoggerFactory.getLogger(BackendApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	CommandLineRunner seedPraias(BeachRepository beachRepository,
	                             BalneabilidadeService balneabilidadeService) {
		return args -> {

			try {
				if (beachRepository.count() == 0) {
					List<Beach> praias = List.of(
						criarPraia("Canto do Forte",   -24.0089, -46.4089, "canto-do-forte.jpg"),
						criarPraia("Boqueirão",        -24.0156, -46.4167, "boqueirao.jpg"),
						criarPraia("Guilhermina",      -24.0256, -46.4289, "guilhermina.jpg"),
						criarPraia("Aviação",          -24.0389, -46.4389, "aviacao.jpg"),
						criarPraia("Tupi",             -24.0489, -46.4456, "tupi.jpg"),
						criarPraia("Ocian",            -24.0589, -46.4556, "ocian.jpg"),
						criarPraia("Mirim",            -24.0689, -46.4622, "mirim.jpg"),
						criarPraia("Maracanã",         -24.0756, -46.4689, "maracana.jpg"),
						criarPraia("Caiçara",          -24.0856, -46.4756, "caicara.jpg"),
						criarPraia("Real",             -24.0922, -46.4822, "real.jpg"),
						criarPraia("Flórida",          -24.0989, -46.4889, "florida.jpg"),
						criarPraia("Solemar",          -24.1056, -46.4956, "solemar.jpg")
					);
					beachRepository.saveAll(praias);
					log.info("Seed concluído: {} praias inseridas.", praias.size());
				} else {
					log.info("Seed ignorado: coleção de praias já contém {} registros.", beachRepository.count());
				}
			} catch (Exception e) {
				log.error("Erro ao executar seed de praias: {}", e.getMessage(), e);
			}

			try {
				log.info("Buscando balneabilidade inicial da CETESB...");
				int atualizadas = balneabilidadeService.atualizarBalneabilidade();
				if (atualizadas > 0) {
					log.info("Balneabilidade inicializada: {} praias atualizadas.", atualizadas);
				} else {
					log.warn("Balneabilidade não pôde ser atualizada na inicialização — " +
					         "verifique a conectividade com o ArcGIS da CETESB.");
				}
			} catch (Exception e) {
				log.error("Erro ao inicializar balneabilidade: {}", e.getMessage());
			}
		};
	}

	private Beach criarPraia(String nome, double latitude, double longitude, String imagem) {
		Beach beach = new Beach();
		beach.setNome(nome);
		beach.setLatitude(latitude);
		beach.setLongitude(longitude);
		beach.setImagem(imagem);
		beach.setBalneabilidade(Balneabilidade.SEM_DADOS);
		beach.setScore(0.0);
		return beach;
	}
}
