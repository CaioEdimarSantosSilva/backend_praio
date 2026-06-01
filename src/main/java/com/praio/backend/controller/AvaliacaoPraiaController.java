package com.praio.backend.controller;

import com.praio.backend.dto.AvaliacaoPraiaRequestDTO;
import com.praio.backend.dto.AvaliacaoPraiaResponseDTO;
import com.praio.backend.service.AvaliacaoPraiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Avaliacoes", description = "Comentarios e fotos enviados por usuarios")
public class AvaliacaoPraiaController {

    private final AvaliacaoPraiaService avaliacaoService;

    public AvaliacaoPraiaController(AvaliacaoPraiaService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @Operation(summary = "Listar avaliacoes da praia")
    @GetMapping("/api/praias/{praiaId}/avaliacoes")
    public ResponseEntity<List<AvaliacaoPraiaResponseDTO>> listarPorPraia(@PathVariable String praiaId) {
        return ResponseEntity.ok(avaliacaoService.listarPorPraia(praiaId));
    }

    @Operation(summary = "Criar avaliacao da praia")
    @PostMapping("/api/praias/{praiaId}/avaliacoes")
    public ResponseEntity<AvaliacaoPraiaResponseDTO> criar(
            @PathVariable String praiaId,
            @Valid @RequestBody AvaliacaoPraiaRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(avaliacaoService.criar(praiaId, authentication.getName(), request));
    }

    @Operation(summary = "Listar minhas avaliacoes")
    @GetMapping("/api/usuario/avaliacoes")
    public ResponseEntity<List<AvaliacaoPraiaResponseDTO>> listarMinhas(Authentication authentication) {
        return ResponseEntity.ok(avaliacaoService.listarMinhas(authentication.getName()));
    }

    @Operation(summary = "Atualizar minha avaliacao")
    @PutMapping("/api/usuario/avaliacoes/{id}")
    public ResponseEntity<AvaliacaoPraiaResponseDTO> atualizarMinha(
            @PathVariable String id,
            @Valid @RequestBody AvaliacaoPraiaRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(avaliacaoService.atualizarMinha(id, authentication.getName(), request));
    }

    @Operation(summary = "Deletar minha avaliacao")
    @DeleteMapping("/api/usuario/avaliacoes/{id}")
    public ResponseEntity<Void> deletarMinha(@PathVariable String id, Authentication authentication) {
        avaliacaoService.deletarMinha(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
