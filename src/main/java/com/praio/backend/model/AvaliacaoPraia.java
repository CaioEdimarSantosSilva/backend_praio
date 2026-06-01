package com.praio.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "avaliacoes_praias")
public class AvaliacaoPraia {

    @Id
    private String id;

    @Indexed
    private String praiaId;

    private String praiaNome;

    @Indexed
    private String usuarioId;

    private String usuarioNome;

    private String mensagem;

    private Integer nota;

    private List<String> imagens = new ArrayList<>();

    private Instant criadaEm = Instant.now();

    private boolean ativo = true;
}
