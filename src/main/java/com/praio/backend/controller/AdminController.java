package com.praio.backend.controller;

import com.praio.backend.dto.AvaliacaoPraiaResponseDTO;
import com.praio.backend.dto.BeachUpdateDTO;
import com.praio.backend.dto.UsuarioResponseDTO;
import com.praio.backend.model.Beach;
import com.praio.backend.repository.BeachRepository;
import com.praio.backend.service.AvaliacaoPraiaService;
import com.praio.backend.service.BalneabilidadeService;
import com.praio.backend.service.BeachService;
import com.praio.backend.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Endpoints administrativos — requer ROLE_ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UsuarioService        usuarioService;
    private final BalneabilidadeService balneabilidadeService;
    private final BeachRepository       beachRepository;
    private final AvaliacaoPraiaService avaliacaoPraiaService;
    private final BeachService          beachService;

    public AdminController(
            UsuarioService usuarioService,
            BalneabilidadeService balneabilidadeService,
            BeachRepository beachRepository,
            AvaliacaoPraiaService avaliacaoPraiaService,
            BeachService beachService) {
        this.usuarioService        = usuarioService;
        this.balneabilidadeService = balneabilidadeService;
        this.beachRepository       = beachRepository;
        this.avaliacaoPraiaService = avaliacaoPraiaService;
        this.beachService          = beachService;
    }

    @Operation(summary = "Listar todos os usuários")
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @Operation(summary = "Alternar status ativo/inativo do usuário")
    @PatchMapping("/usuarios/{id}/toggle-ativo")
    public ResponseEntity<UsuarioResponseDTO> toggleAtivo(@PathVariable String id) {
        return ResponseEntity.ok(usuarioService.alternarAtivo(id));
    }

    @Operation(summary = "Deletar usuário")
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable String id) {
        usuarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Forçar atualização de dados climáticos (Open-Meteo)")
    @PostMapping("/atualizacoes/clima")
    public ResponseEntity<Map<String, Object>> atualizarClima() {
        return ResponseEntity.ok(beachService.forcarAtualizacaoClima());
    }

    @Operation(summary = "Forçar atualização de dados marítimos (Open-Meteo Marine)")
    @PostMapping("/atualizacoes/maritimo")
    public ResponseEntity<Map<String, Object>> atualizarMaritimo() {
        return ResponseEntity.ok(beachService.forcarAtualizacaoMaritimo());
    }

    @Operation(summary = "Forçar atualização de qualidade do ar (CETESB QUALAR)")
    @PostMapping("/atualizacoes/ar")
    public ResponseEntity<Map<String, Object>> atualizarAr() {
        return ResponseEntity.ok(beachService.forcarAtualizacaoAr());
    }

    @Operation(summary = "Forçar atualização de balneabilidade (CETESB ArcGIS)")
    @PostMapping("/atualizacoes/balneabilidade")
    public ResponseEntity<Map<String, Object>> atualizarBalneabilidade() {
        int atualizadas = balneabilidadeService.atualizarBalneabilidade();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "praias_atualizadas", atualizadas,
                "total", 12,
                "fonte", "CETESB ArcGIS Feature Layer",
                "erros", List.of()
        ));
    }

    @Operation(summary = "Forçar atualização completa de dados ambientais")
    @PostMapping("/atualizacoes/tudo")
    public ResponseEntity<Map<String, Object>> atualizarTudo() {
        return ResponseEntity.ok(beachService.forcarAtualizacaoCompleta());
    }

    @PostMapping("/balneabilidade/atualizar")
    public ResponseEntity<Map<String, Object>> atualizarBalneabilidadeLegado() {
        int atualizadas = balneabilidadeService.atualizarBalneabilidade();
        return ResponseEntity.ok(Map.of("status", "ok", "praias_atualizadas", atualizadas,
                                        "fonte", "CETESB ArcGIS Feature Layer"));
    }

    @Operation(summary = "Listar praias para edição")
    @GetMapping("/praias")
    public ResponseEntity<List<Beach>> listarPraias() {
        return ResponseEntity.ok(beachRepository.findAll());
    }

    @Operation(summary = "Buscar praia por ID")
    @GetMapping("/praias/{id}")
    public ResponseEntity<Beach> buscarPraia(@PathVariable String id) {
        return beachRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Atualizar praia")
    @PutMapping("/praias/{id}")
    public ResponseEntity<Beach> atualizarPraia(@PathVariable String id, @RequestBody BeachUpdateDTO dto) {
        try {
            return ResponseEntity.ok(beachService.atualizarPraia(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Listar avaliações")
    @GetMapping("/avaliacoes")
    public ResponseEntity<List<AvaliacaoPraiaResponseDTO>> listarAvaliacoes() {
        return ResponseEntity.ok(avaliacaoPraiaService.listarTodas());
    }

    @Operation(summary = "Alternar visibilidade da avaliação")
    @PatchMapping("/avaliacoes/{id}/toggle-ativo")
    public ResponseEntity<AvaliacaoPraiaResponseDTO> toggleAtivoAvaliacao(@PathVariable String id) {
        return ResponseEntity.ok(avaliacaoPraiaService.toggleAtivo(id));
    }

    @Operation(summary = "Deletar avaliação")
    @DeleteMapping("/avaliacoes/{id}")
    public ResponseEntity<Void> deletarAvaliacao(@PathVariable String id) {
        avaliacaoPraiaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
