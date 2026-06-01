package com.praio.backend.service;

import com.praio.backend.dto.UsuarioResponseDTO;
import com.praio.backend.dto.UsuarioUpdateDTO;
import com.praio.backend.model.Usuario;
import com.praio.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private static final long MAX_FOTO_BYTES = 2 * 1024 * 1024;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;

    @Value("${praio.uploads.dir:uploads}")
    private String uploadsDir;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder   = passwordEncoder;
    }

    public UsuarioResponseDTO buscarPerfil(String email) {
        return toDTO(buscarPorEmail(email));
    }

    public UsuarioResponseDTO atualizarPerfil(String email, UsuarioUpdateDTO update) {
        Usuario usuario = buscarPorEmail(email);

        if (update.nome() != null && !update.nome().isBlank()) {
            usuario.setNome(update.nome());
        }
        if (update.novaSenha() != null && !update.novaSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(update.novaSenha()));
        }
        if (update.foto() != null && !update.foto().isBlank()) {

            String nomeEfetivo = (update.nome() != null && !update.nome().isBlank())
                    ? update.nome()
                    : usuario.getNome();
            String fotoPath = salvarFoto(nomeEfetivo, usuario.getFotoPath(), update.foto());
            usuario.setFotoPath(fotoPath);
        }

        usuarioRepository.save(usuario);
        log.info("Perfil atualizado: {}", email);
        return toDTO(usuario);
    }

    public UsuarioResponseDTO adicionarFavorito(String email, String praiaId) {
        Usuario usuario = buscarPorEmail(email);
        if (!usuario.getFavoritosIds().contains(praiaId)) {
            usuario.getFavoritosIds().add(praiaId);
            usuarioRepository.save(usuario);
            log.debug("Favorito adicionado: {} → praia {}", email, praiaId);
        }
        return toDTO(usuario);
    }

    public UsuarioResponseDTO removerFavorito(String email, String praiaId) {
        Usuario usuario = buscarPorEmail(email);
        usuario.getFavoritosIds().remove(praiaId);
        usuarioRepository.save(usuario);
        log.debug("Favorito removido: {} → praia {}", email, praiaId);
        return toDTO(usuario);
    }

    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public UsuarioResponseDTO alternarAtivo(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + id));
        usuario.setAtivo(!usuario.isAtivo());
        usuarioRepository.save(usuario);
        log.info("Status do usuário {} alterado para ativo={}", id, usuario.isAtivo());
        return toDTO(usuario);
    }

    public void deletar(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + id));

        if (usuario.getFotoPath() != null) {
            removerArquivo(usuario.getFotoPath());
        }
        usuarioRepository.deleteById(id);
        log.info("Usuário deletado: {}", id);
    }

    private String salvarFoto(String nomeUsuario, String fotoPathAtual, String base64) {

        String ext  = detectarExtensao(base64);
        String dados = base64.contains(",") ? base64.split(",", 2)[1] : base64;

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dados);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Imagem inválida: formato base64 incorreto.");
        }

        if (bytes.length > MAX_FOTO_BYTES) {
            throw new IllegalArgumentException("Imagem muito grande. Tamanho máximo: 2 MB.");
        }

        if (fotoPathAtual != null) {
            removerArquivo(fotoPathAtual);
        }

        String slug     = slugNome(nomeUsuario);
        String uid      = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String arquivo  = slug + "_" + uid + "." + ext;
        String caminho  = "avatars/" + arquivo;

        Path dir     = Paths.get(uploadsDir, "avatars");
        Path destino = dir.resolve(arquivo);

        try {
            Files.createDirectories(dir);
            Files.write(destino, bytes);
            log.info("Foto salva: {}", destino.toAbsolutePath());
        } catch (IOException e) {
            log.error("Erro ao salvar foto de {}: {}", nomeUsuario, e.getMessage());
            throw new RuntimeException("Não foi possível salvar a foto. Tente novamente.");
        }

        return caminho;
    }

    private String slugNome(String nome) {

        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return semAcento.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private void removerArquivo(String fotoPath) {
        try {
            Path arquivo = Paths.get(uploadsDir, fotoPath);
            Files.deleteIfExists(arquivo);
        } catch (IOException e) {
            log.warn("Não foi possível remover a foto antiga: {}", e.getMessage());
        }
    }

    private String detectarExtensao(String base64) {
        if (base64.startsWith("data:image/png"))  return "png";
        if (base64.startsWith("data:image/webp")) return "webp";
        return "jpg";
    }

    private Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + email));
    }

    public UsuarioResponseDTO toDTO(Usuario u) {
        String fotoUrl = u.getFotoPath() != null
                ? "/uploads/" + u.getFotoPath()
                : null;
        return new UsuarioResponseDTO(
                u.getId(), u.getNome(), u.getEmail(),
                u.getRoles(), u.isAtivo(), u.getFavoritosIds(),
                u.getDataCriacao(), fotoUrl);
    }
}
