package br.com.senai.agenciaviagens.controller;

import br.com.senai.agenciaviagens.model.AvaliacaoRequest;
import br.com.senai.agenciaviagens.model.Destino;
import br.com.senai.agenciaviagens.service.DestinoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destinos")
public class DestinoController {

    private final DestinoService destinoService;

    public DestinoController(DestinoService destinoService) {
        this.destinoService = destinoService;
    }

    @PostMapping
    public ResponseEntity<Destino> cadastrar(@Valid @RequestBody Destino destino) {
        Destino salvo = destinoService.cadastrar(destino);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping
    public ResponseEntity<List<Destino>> listarTodos() {
        return ResponseEntity.ok(destinoService.listarTodos());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Destino>> pesquisar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String localizacao) {
        return ResponseEntity.ok(destinoService.pesquisar(nome, localizacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Destino> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(destinoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Destino> atualizar(@PathVariable Long id,
                                             @Valid @RequestBody Destino destino) {
        return ResponseEntity.ok(destinoService.atualizar(id, destino));
    }

    @PatchMapping("/{id}/avaliacoes")
    public ResponseEntity<Destino> registrarAvaliacao(@PathVariable Long id,
                                                      @Valid @RequestBody AvaliacaoRequest avaliacao) {
        Destino atualizado = destinoService.registrarAvaliacao(id, avaliacao.getNota());
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        destinoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
