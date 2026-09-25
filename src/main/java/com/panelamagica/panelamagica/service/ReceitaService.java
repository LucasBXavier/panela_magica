package com.panelamagica.panelamagica.service;

import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.dto.receitas.ReceitaRequestDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaResponseDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitasUpdateDTO;
import com.panelamagica.panelamagica.exception.BusinessRuleException;
import com.panelamagica.panelamagica.exception.ResourceNotFoundException;
import com.panelamagica.panelamagica.mapper.ReceitaMapper;
import com.panelamagica.panelamagica.repository.ReceitasRepository;
import com.panelamagica.panelamagica.repository.UsuarioRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ReceitaService {
    private ReceitasRepository receitasRepository;
    private ReceitaMapper receitaMapper;
    private UsuarioRepository userRepository;

    private Usuario getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessRuleException("Usuário não autenticado");
        }

        String email = authentication.getName();
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException("Usuário autenticado não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<ReceitaResponseDTO> getReceitas() {
        return receitasRepository.findAll().stream()
                .map(receitaMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReceitaResponseDTO getReceitasById(UUID id) {
        return receitasRepository.findById(id)
                .map(receitaMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Receita não encontrada"));
    }

    @Transactional
    public ReceitaResponseDTO criarReceita(ReceitaRequestDTO dto) {
        Usuario usuario = getAuthenticatedUser();

        if (receitasRepository.existsByNomeAndUsuarioId(dto.getNome(), usuario.getId())) {
            throw new BusinessRuleException("Receita já cadastrada para o usuário logado");
        }

        var receita = receitaMapper.toEntity(dto);
        receita.setUsuario(usuario);
        return receitaMapper.toResponseDTO(receitasRepository.save(receita));
    }

    @Transactional
    public void deletarReceita(UUID id) {
        Usuario usuario = getAuthenticatedUser();

        if (!receitasRepository.existsById(id)) {
            throw new ResourceNotFoundException("Receita não encontrada");
        }

        if (!receitasRepository.existsByIdAndUsuarioId(id, usuario.getId())) {
            throw new BusinessRuleException("Receita não pertence ao usuário autenticado");
        }

        receitasRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ReceitaResponseDTO> minhasReceitas() {
        Usuario usuario = getAuthenticatedUser();

        return receitasRepository.findAllByUsuario(usuario).stream()
                .map(receitaMapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public String uploadImagemReceita(UUID receitaId, MultipartFile file) {
        Usuario usuario = getAuthenticatedUser();

        var receita = receitasRepository.findById(receitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Receita não encontrada"));

        if (!receita.getUsuario().getId().equals(usuario.getId())) {
            throw new BusinessRuleException("Receita não pertence ao usuário autenticado");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Arquivo de imagem não informado");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessRuleException("Erro ao processar a imagem da receita");
        }

        receita.setImagem(bytes);
        receita.setImagemContentType(detectarTipoImagem(bytes));
        receitasRepository.save(receita);
        return "/api/v1/receitas/imagem/" + receitaId;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getImagemReceita(UUID receitaId) {
        var receita = receitasRepository.findById(receitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Receita não encontrada"));

        if (receita.getImagem() == null) {
            throw new ResourceNotFoundException("Receita não possui imagem");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(receita.getImagemContentType()))
                .contentLength(receita.getImagem().length)
                .body(receita.getImagem());
    }

    /** Identifica o tipo pelos primeiros bytes (magic numbers), sem confiar no Content-Type do cliente. */
    private String detectarTipoImagem(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        throw new BusinessRuleException("Formato de imagem inválido. Formatos aceitos: JPEG, PNG e WEBP");
    }

    @Transactional
    public ReceitaResponseDTO atualizarReceita(UUID id, ReceitasUpdateDTO dto) {
        Usuario usuario = getAuthenticatedUser();

        var receita = receitasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receita não encontrada"));

        if (!receita.getUsuario().getId().equals(usuario.getId())) {
            throw new BusinessRuleException("Receita não pertence ao usuário autenticado");
        }

        if (dto.getNome() != null && !dto.getNome().equals(receita.getNome())
                && receitasRepository.existsByNomeAndUsuarioId(dto.getNome(), usuario.getId())) {
            throw new BusinessRuleException("Receita já cadastrada para o usuário logado");
        }

        receitaMapper.updateEntityFromDTO(dto, receita);
        return receitaMapper.toResponseDTO(receitasRepository.save(receita));
    }

    @Transactional
    public void deletarImagemReceita(UUID receitaId) {
        Usuario usuario = getAuthenticatedUser();

        var receita = receitasRepository.findById(receitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Receita não encontrada"));

        if (!receita.getUsuario().getId().equals(usuario.getId())) {
            throw new BusinessRuleException("Receita não pertence ao usuário autenticado");
        }

        if (receita.getImagem() == null) {
            throw new ResourceNotFoundException("Receita não possui imagem para deletar");
        }

        receita.setImagem(null);
        receita.setImagemContentType(null);
        receitasRepository.save(receita);
    }
}
