package com.panelamagica.panelamagica.service;

import com.panelamagica.panelamagica.domain.entites.ReceitaImagem;
import com.panelamagica.panelamagica.domain.entites.Receitas;
import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.dto.receitas.ReceitaRequestDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaResponseDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitasUpdateDTO;
import com.panelamagica.panelamagica.exception.BusinessRuleException;
import com.panelamagica.panelamagica.exception.ForbiddenException;
import com.panelamagica.panelamagica.exception.ResourceNotFoundException;
import com.panelamagica.panelamagica.exception.UnauthorizedException;
import com.panelamagica.panelamagica.mapper.ReceitaMapper;
import com.panelamagica.panelamagica.repository.ReceitasRepository;
import com.panelamagica.panelamagica.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ReceitaService {
    private final ReceitasRepository receitasRepository;
    private final ReceitaMapper receitaMapper;
    private final UsuarioRepository userRepository;

    private Usuario getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Usuário não autenticado");
        }

        // o subject do JWT é o id (UUID) do usuário; tokens antigos (subject = e-mail) exigem novo login
        UUID usuarioId;
        try {
            usuarioId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Token inválido: faça login novamente");
        }
        return userRepository
                .findById(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Usuário autenticado não encontrado"));
    }

    /**
     * Carrega a receita e garante que pertence ao usuário: 404 se não existe, 403 se é de outro usuário.
     * (As receitas são públicas para leitura, então revelar que existem não expõe nada; por isso 403 e não 404.)
     */
    private Receitas buscarDoUsuario(UUID id, Usuario usuario) {
        var receita = receitasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receita não encontrada"));

        if (!receita.getUsuario().getId().equals(usuario.getId())) {
            throw new ForbiddenException("Receita não pertence ao usuário autenticado");
        }
        return receita;
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

        receitasRepository.delete(buscarDoUsuario(id, usuario));
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

        var receita = buscarDoUsuario(receitaId, usuario);

        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Arquivo de imagem não informado");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessRuleException("Erro ao processar a imagem da receita");
        }

        String contentType = detectarTipoImagem(bytes);
        ReceitaImagem imagem = receita.getImagem() != null ? receita.getImagem() : new ReceitaImagem();
        imagem.setDados(bytes);
        imagem.setContentType(contentType);
        receita.setImagem(imagem);
        receitasRepository.save(receita);
        return ReceitaMapper.imagemUrl(receitaId);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getImagemReceita(UUID receitaId) {
        var receita = receitasRepository.findById(receitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Receita não encontrada"));

        ReceitaImagem imagem = receita.getImagem();
        if (imagem == null) {
            throw new ResourceNotFoundException("Receita não possui imagem");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.getContentType()))
                .contentLength(imagem.getDados().length)
                .body(imagem.getDados());
    }

    /**
     * Identifica o tipo pelos primeiros bytes (magic numbers), sem confiar no Content-Type do cliente.
     */
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

        var receita = buscarDoUsuario(id, usuario);

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

        var receita = buscarDoUsuario(receitaId, usuario);

        if (receita.getImagem() == null) {
            throw new ResourceNotFoundException("Receita não possui imagem para deletar");
        }

        receita.setImagem(null);
        receitasRepository.save(receita);
    }
}
