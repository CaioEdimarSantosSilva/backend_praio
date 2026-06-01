package com.praio.backend.controller;

import com.praio.backend.dto.auth.CadastroRequestDTO;
import com.praio.backend.dto.auth.LoginRequestDTO;
import com.praio.backend.dto.auth.LoginResponseDTO;
import com.praio.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints de login e cadastro de usuários")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login", description = "Autentica com email e senha, retorna token JWT")
    @ApiResponse(responseCode = "200", description = "Login bem-sucedido")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            LoginResponseDTO response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "Email ou senha inválidos"));
        }
    }

    @Operation(summary = "Cadastro", description = "Cria uma nova conta de usuário")
    @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso")
    @ApiResponse(responseCode = "400", description = "Email já cadastrado ou dados inválidos")
    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrar(@Valid @RequestBody CadastroRequestDTO request) {
        try {
            LoginResponseDTO response = authService.cadastrar(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erro", e.getMessage()));
        }
    }
}
