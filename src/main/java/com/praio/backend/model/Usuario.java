package com.praio.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    private String nome;

    @Indexed(unique = true)
    private String email;

    private String senha;

    private Set<String> roles;

    private boolean ativo = true;

    private List<String> favoritosIds = new ArrayList<>();

    private String fotoPath;

    private Instant dataCriacao = Instant.now();
}
