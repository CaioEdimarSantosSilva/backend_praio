package com.praio.backend.repository;

import com.praio.backend.model.AvaliacaoPraia;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvaliacaoPraiaRepository extends MongoRepository<AvaliacaoPraia, String> {

    List<AvaliacaoPraia> findByPraiaIdAndAtivoTrueOrderByCriadaEmDesc(String praiaId);

    List<AvaliacaoPraia> findByPraiaIdOrderByCriadaEmDesc(String praiaId);

    List<AvaliacaoPraia> findByUsuarioIdOrderByCriadaEmDesc(String usuarioId);
}
