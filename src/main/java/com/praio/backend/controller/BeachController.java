package com.praio.backend.controller;

import com.praio.backend.dto.BeachResponseDTO;
import com.praio.backend.dto.BeachSummaryDTO;
import com.praio.backend.model.Balneabilidade;
import com.praio.backend.service.BalneabilidadeService;
import com.praio.backend.service.BeachService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/praias")
@Tag(name = "Praias", description = "Endpoints de consulta das 12 praias de Praia Grande SP")
public class BeachController {

    private final BeachService beachService;
    private final BalneabilidadeService balneabilidadeService;

    public BeachController(BeachService beachService, BalneabilidadeService balneabilidadeService) {
        this.beachService          = beachService;
        this.balneabilidadeService = balneabilidadeService;
    }

    @Operation(summary = "Listar todas as praias", description = "Retorna as 12 praias fixas de Praia Grande SP")
    @ApiResponse(responseCode = "200", description = "Lista de praias retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<BeachSummaryDTO>> listarTodas() {
        return ResponseEntity.ok(beachService.listarTodas());
    }

    @Operation(summary = "Detalhes de uma praia", description = "Retorna todos os dados de monitoramento de uma praia específica")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Praia encontrada"),
        @ApiResponse(responseCode = "404", description = "Praia não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BeachResponseDTO> buscarPorId(
            @Parameter(description = "ID MongoDB da praia") @PathVariable String id) {
        return ResponseEntity.ok(beachService.buscarPorId(id));
    }

    @Operation(summary = "Filtrar por balneabilidade", description = "Lista praias com o status de balneabilidade especificado")
    @ApiResponse(responseCode = "200", description = "Praias filtradas com sucesso")
    @GetMapping("/balneabilidade/{balneabilidade}")
    public ResponseEntity<List<BeachSummaryDTO>> listarPorBalneabilidade(
            @Parameter(description = "Status de balneabilidade: PROPRIA, IMPROPRIA ou SEM_DADOS")
            @PathVariable Balneabilidade balneabilidade) {
        return ResponseEntity.ok(beachService.listarPorBalneabilidade(balneabilidade));
    }

    @Operation(summary = "Melhores praias", description = "Lista praias com score acima do mínimo, em ordem decrescente")
    @ApiResponse(responseCode = "200", description = "Melhores praias retornadas com sucesso")
    @GetMapping("/melhores")
    public ResponseEntity<List<BeachSummaryDTO>> listarMelhores(
            @Parameter(description = "Score mínimo (padrão: 7.0)")
            @RequestParam(defaultValue = "7.0") double scoreMinimo) {
        return ResponseEntity.ok(beachService.listarMelhoresPraias(scoreMinimo));
    }

    @Operation(
        summary = "Atualizar balneabilidade",
        description = "Consulta o ArcGIS da CETESB e atualiza a balneabilidade de todas as praias de Praia Grande"
    )
    @ApiResponse(responseCode = "200", description = "Atualização executada com sucesso")
    @PostMapping("/atualizar-balneabilidade")
    public ResponseEntity<Map<String, Object>> atualizarBalneabilidade() {
        int atualizadas = balneabilidadeService.atualizarBalneabilidade();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "praias_atualizadas", atualizadas,
                "fonte", "CETESB ArcGIS Feature Layer"
        ));
    }
}
