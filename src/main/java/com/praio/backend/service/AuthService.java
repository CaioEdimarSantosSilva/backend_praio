package com.praio.backend.service;

import com.praio.backend.dto.auth.CadastroRequestDTO;
import com.praio.backend.dto.auth.LoginRequestDTO;
import com.praio.backend.dto.auth.LoginResponseDTO;
import com.praio.backend.model.Usuario;
import com.praio.backend.repository.UsuarioRepository;
import com.praio.backend.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository   usuarioRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtUtil             jwtUtil;
    private final AuthenticationManager authManager;
    private final UserDetailsService  userDetailsService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authManager,
            UserDetailsService userDetailsService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder   = passwordEncoder;
        this.jwtUtil           = jwtUtil;
        this.authManager       = authManager;
        this.userDetailsService = userDetailsService;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtUtil.gerarToken(userDetails);

        Usuario usuario = usuarioRepository.findByEmail(request.email()).orElseThrow();
        log.info("Login bem-sucedido: {}", request.email());

        return new LoginResponseDTO(token, usuario.getId(), usuario.getNome(),
                usuario.getEmail(), usuario.getRoles());
    }

    public LoginResponseDTO cadastrar(CadastroRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email já cadastrado: " + request.email());
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setRoles(Set.of("ROLE_USER"));

        usuarioRepository.save(usuario);
        log.info("Novo usuário cadastrado: {}", request.email());

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtUtil.gerarToken(userDetails);

        return new LoginResponseDTO(token, usuario.getId(), usuario.getNome(),
                usuario.getEmail(), usuario.getRoles());
    }
}
