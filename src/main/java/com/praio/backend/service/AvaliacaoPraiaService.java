package com.praio.backend.service;

import com.praio.backend.dto.AvaliacaoPraiaRequestDTO;
import com.praio.backend.dto.AvaliacaoPraiaResponseDTO;
import com.praio.backend.exception.BeachNotFoundException;
import com.praio.backend.model.AvaliacaoPraia;
import com.praio.backend.model.Beach;
import com.praio.backend.model.Usuario;
import com.praio.backend.repository.AvaliacaoPraiaRepository;
import com.praio.backend.repository.BeachRepository;
import com.praio.backend.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AvaliacaoPraiaService {

    private final AvaliacaoPraiaRepository avaliacaoRepository;
    private final BeachRepository beachRepository;
    private final UsuarioRepository usuarioRepository;

    public AvaliacaoPraiaService(
            AvaliacaoPraiaRepository avaliacaoRepository,
            BeachRepository beachRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.beachRepository = beachRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<AvaliacaoPraiaResponseDTO> listarPorPraia(String praiaId) {
        validarPraia(praiaId);
        return avaliacaoRepository.findByPraiaIdAndAtivoTrueOrderByCriadaEmDesc(praiaId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AvaliacaoPraiaResponseDTO> listarTodas() {
        return avaliacaoRepository.findAll(Sort.by(Sort.Direction.DESC, "criadaEm")).stream()
                .map(this::toResponse)
                .toList();
    }

    public AvaliacaoPraiaResponseDTO criar(String praiaId, String email, AvaliacaoPraiaRequestDTO request) {
        Beach praia = beachRepository.findById(praiaId)
                .orElseThrow(() -> new BeachNotFoundException(praiaId));
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));

        AvaliacaoPraia avaliacao = new AvaliacaoPraia();
        avaliacao.setPraiaId(praia.getId());
        avaliacao.setPraiaNome(praia.getNome());
        avaliacao.setUsuarioId(usuario.getId());
        avaliacao.setUsuarioNome(usuario.getNome());
        avaliacao.setMensagem(request.mensagem().trim());
        avaliacao.setNota(request.nota());
        avaliacao.setImagens(request.imagens() == null ? List.of() : request.imagens());

        return toResponse(avaliacaoRepository.save(avaliacao));
    }

    public List<AvaliacaoPraiaResponseDTO> listarMinhas(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));
        return avaliacaoRepository.findByUsuarioIdOrderByCriadaEmDesc(usuario.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public AvaliacaoPraiaResponseDTO atualizarMinha(String id, String email, AvaliacaoPraiaRequestDTO request) {
        Usuario usuario = buscarUsuarioPorEmail(email);
        AvaliacaoPraia avaliacao = buscarAvaliacaoDoUsuario(id, usuario);

        avaliacao.setMensagem(request.mensagem().trim());
        avaliacao.setNota(request.nota());
        avaliacao.setImagens(request.imagens() == null ? List.of() : request.imagens());

        return toResponse(avaliacaoRepository.save(avaliacao));
    }

    public void deletarMinha(String id, String email) {
        Usuario usuario = buscarUsuarioPorEmail(email);
        AvaliacaoPraia avaliacao = buscarAvaliacaoDoUsuario(id, usuario);
        avaliacaoRepository.delete(avaliacao);
    }

    public AvaliacaoPraiaResponseDTO toggleAtivo(String id) {
        AvaliacaoPraia avaliacao = avaliacaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Avaliacao nao encontrada: " + id));
        avaliacao.setAtivo(!avaliacao.isAtivo());
        return toResponse(avaliacaoRepository.save(avaliacao));
    }

    public void deletar(String id) {
        avaliacaoRepository.deleteById(id);
    }

    private Usuario buscarUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));
    }

    private AvaliacaoPraia buscarAvaliacaoDoUsuario(String id, Usuario usuario) {
        AvaliacaoPraia avaliacao = avaliacaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Avaliacao nao encontrada: " + id));

        if (!usuario.getId().equals(avaliacao.getUsuarioId())) {
            throw new AccessDeniedException("Usuario nao pode alterar esta avaliacao.");
        }

        return avaliacao;
    }

    private void validarPraia(String praiaId) {
        if (!beachRepository.existsById(praiaId)) {
            throw new BeachNotFoundException(praiaId);
        }
    }

    private AvaliacaoPraiaResponseDTO toResponse(AvaliacaoPraia avaliacao) {

        String usuarioFoto = usuarioRepository.findById(avaliacao.getUsuarioId())
                .map(u -> u.getFotoPath() != null ? "/uploads/" + u.getFotoPath() : null)
                .orElse(null);

        return new AvaliacaoPraiaResponseDTO(
                avaliacao.getId(),
                avaliacao.getPraiaId(),
                avaliacao.getPraiaNome(),
                avaliacao.getUsuarioId(),
                avaliacao.getUsuarioNome(),
                usuarioFoto,
                avaliacao.getMensagem(),
                avaliacao.getNota(),
                avaliacao.getImagens() == null ? List.of() : avaliacao.getImagens(),
                avaliacao.getCriadaEm(),
                avaliacao.isAtivo()
        );
    }
}
