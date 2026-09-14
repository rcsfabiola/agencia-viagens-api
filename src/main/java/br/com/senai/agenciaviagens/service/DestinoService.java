package br.com.senai.agenciaviagens.service;

import br.com.senai.agenciaviagens.exception.DestinoNaoEncontradoException;
import br.com.senai.agenciaviagens.model.Destino;
import br.com.senai.agenciaviagens.repository.DestinoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DestinoService {

    private final DestinoRepository destinoRepository;

    public DestinoService(DestinoRepository destinoRepository) {
        this.destinoRepository = destinoRepository;
        popularDadosIniciais();
    }

    public Destino cadastrar(Destino destino) {
        return destinoRepository.save(destino);
    }

    public List<Destino> listarTodos() {
        return destinoRepository.findAll(Sort.by("id"));
    }

    public List<Destino> pesquisar(String nome, String localizacao) {
        return destinoRepository.findAll(Sort.by("id")).stream()
                .filter(d -> nome == null || nome.isBlank()
                        || d.getNome().toLowerCase().contains(nome.toLowerCase()))
                .filter(d -> localizacao == null || localizacao.isBlank()
                        || d.getLocalizacao().toLowerCase().contains(localizacao.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Destino buscarPorId(Long id) {
        return destinoRepository.findById(id)
                .orElseThrow(() -> new DestinoNaoEncontradoException(id));
    }

    public Destino atualizar(Long id, Destino dadosAtualizados) {
        Destino existente = buscarPorId(id);

        existente.setNome(dadosAtualizados.getNome());
        existente.setLocalizacao(dadosAtualizados.getLocalizacao());
        existente.setDescricao(dadosAtualizados.getDescricao());
        existente.setHoteisDisponiveis(dadosAtualizados.getHoteisDisponiveis());
        existente.setAtividadesTuristicas(dadosAtualizados.getAtividadesTuristicas());

        return destinoRepository.save(existente);
    }

    public Destino registrarAvaliacao(Long id, int nota) {
        Destino destino = buscarPorId(id);
        destino.adicionarAvaliacao(nota);
        return destinoRepository.save(destino);
    }

    public void excluir(Long id) {
        if (!destinoRepository.existsById(id)) {
            throw new DestinoNaoEncontradoException(id);
        }
        destinoRepository.deleteById(id);
    }

    private void popularDadosIniciais() {
        if (destinoRepository.count() > 0) {
            return;
        }

        Destino floripa = new Destino(null, "Florianópolis", "Santa Catarina, Brasil",
                "Ilha da Magia: praias, dunas e gastronomia", 12,
                List.of("Trilha da Lagoinha do Leste", "Passeio de barco", "Surf na Joaquina"));
        floripa.adicionarAvaliacao(5);
        floripa.adicionarAvaliacao(4);
        cadastrar(floripa);

        Destino gramado = new Destino(null, "Gramado", "Rio Grande do Sul, Brasil",
                "Clima de serra, chocolate e arquitetura europeia", 20,
                List.of("Mini Mundo", "Snowland", "Rota do Vinho"));
        gramado.adicionarAvaliacao(5);
        gramado.adicionarAvaliacao(5);
        gramado.adicionarAvaliacao(4);
        cadastrar(gramado);
    }
}