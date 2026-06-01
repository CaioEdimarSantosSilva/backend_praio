package com.praio.backend.dto;

import jakarta.validation.constraints.Size;

public record UsuarioUpdateDTO(

        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String nome,

        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String novaSenha,

        String foto
) {}
