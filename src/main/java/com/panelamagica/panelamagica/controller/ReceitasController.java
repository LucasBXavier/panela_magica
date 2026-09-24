package com.panelamagica.panelamagica.controller;


import com.panelamagica.panelamagica.dto.receitas.ReceitaRequestDTO;
import com.panelamagica.panelamagica.service.ReceitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receitas")
@RequiredArgsConstructor
public class ReceitasController {

    private final ReceitaService service;

    @GetMapping
    public ResponseEntity<?> getReceitas(){
        service.getReceitas();
        return ResponseEntity.ok().body("Lista de receitas");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReceitaById(@Valid @PathVariable UUID id){
        service.getReceitasById(id);
        return ResponseEntity.ok().body("Receita com ID: " + id);
    }

    @PostMapping("/criar")
    public ResponseEntity<?> criarReceita(@Valid @RequestBody ReceitaRequestDTO dto){
        service.criarReceita(dto);
        return ResponseEntity.ok().body("Receita criada com sucesso");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarReceita(@Valid @PathVariable UUID id){
        service.deletarReceita(id);
        return ResponseEntity.ok().body("Receita com ID: " + id + " deletada com sucesso");
    }
}
