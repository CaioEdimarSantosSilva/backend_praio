package com.praio.backend.controller;

import com.praio.backend.dto.UsuarioResponseDTO;
import com.praio.backend.dto.UsuarioUpdateDTO;
import com.praio.backend.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuario")
@Tag(name = "Usuário", description = "Endpoints de perfil e favoritos do usuário autenticado")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Meu perfil")
    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponseDTO> perfil(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(usuarioService.buscarPerfil(userDetails.getUsername()));
    }

    @Operation(summary = "Atualizar perfil")
    @PutMapping("/perfil")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UsuarioUpdateDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarPerfil(userDetails.getUsername(), dto));
    }

    @Operation(summary = "Favoritar praia")
    @PostMapping("/favoritos/{praiaId}")
    public ResponseEntity<UsuarioResponseDTO> favoritar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String praiaId) {
        return ResponseEntity.ok(usuarioService.adicionarFavorito(userDetails.getUsername(), praiaId));
    }

    @Operation(summary = "Desfavoritar praia")
    @DeleteMapping("/favoritos/{praiaId}")
    public ResponseEntity<UsuarioResponseDTO> desfavoritar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String praiaId) {
        return ResponseEntity.ok(usuarioService.removerFavorito(userDetails.getUsername(), praiaId));
    }
}
