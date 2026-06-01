package com.praio.backend.repository;

import com.praio.backend.model.Beach;
import com.praio.backend.model.Balneabilidade;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeachRepository extends MongoRepository<Beach, String> {

    Optional<Beach> findByNome(String nome);

    List<Beach> findByBalneabilidade(Balneabilidade balneabilidade);

    List<Beach> findByScoreGreaterThanEqualOrderByScoreDesc(double score);
}
