package com.praio.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste de carregamento do contexto Spring Boot.
 * Verifica se todos os beans são criados sem erros na inicialização.
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

	@Test
	void contextLoads() {
		// Verifica que o contexto Spring sobe sem erros
	}
}
