package com.panelamagica.panelamagica.service;

import com.panelamagica.panelamagica.dto.receitas.ReceitaRequestDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaResponseDTO;
import com.panelamagica.panelamagica.mapper.ReceitaMapper;
import com.panelamagica.panelamagica.repository.ReceitasRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ReceitaService {
    private ReceitasRepository receitasRepository;
    private ReceitaMapper receitaMapper;
    private UsuarioService usuarioService;

    public ReceitaResponseDTO getReceitas() {
       var receitas =  receitasRepository.findAll();
         return receitas.stream()
                 .map(receitaMapper::toResponseDTO)
                 .findFirst()
                 .orElse(null);
    }

    public void getReceitasById(UUID id) {
        var receita = receitasRepository.findById(id);
        receita.map(receitaMapper::toResponseDTO);
    }

    public void criarReceita(ReceitaRequestDTO dto) {
        var receita = receitaMapper.toEntity(dto);
        receita.setUsuario(usuarioService.getUsuarioAutenticado());
        receitasRepository.save(receita);
    }

    public void deletarReceita(UUID id) {
        receitasRepository.deleteById(id);
    }
}
