package com.praio.backend.config;

import com.praio.backend.repository.BeachRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BeachRepository beachRepository;

    public DataInitializer(BeachRepository beachRepository) {
        this.beachRepository = beachRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        beachRepository.findByNome("Praião").ifPresent(p -> {
            beachRepository.delete(p);
            log.info("Praia mockada 'Praião' removida do MongoDB.");
        });
    }
}
