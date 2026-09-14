package br.com.senai.agenciaviagens.service;

import br.com.senai.agenciaviagens.exception.DestinoNaoEncontradoException;
import br.com.senai.agenciaviagens.model.Destino;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DestinoService {

    private final Map<Long, Destino> destinos = new ConcurrentHashMap<>();

    private final AtomicLong proximoId = new AtomicLong(1);

    public DestinoService() {
        popularDadosIniciais();
    }

    public Destino cadastrar(Destino destino) {
        Long id = proximoId.getAndIncrement();
        destino.setId(id);
        destinos.put(id, destino);
        return destino;
    }

    public List<Destino> listarTodos() {
        // ConcurrentHashMap nao mantem ordem de insercao
        return destinos.values().stream()
                .sorted(Comparator.comparing(Destino::getId))
                .collect(Collectors.toList());
    }

    public List<Destino> pesquisar(String nome, String localizacao) {
        return destinos.values().stream()
                .filter(d -> nome == null || nome.isBlank()
                        || d.getNome().toLowerCase().contains(nome.toLowerCase()))
                .filter(d -> localizacao == null || localizacao.isBlank()
                        || d.getLocalizacao().toLowerCase().contains(localizacao.toLowerCase()))
                .sorted(Comparator.comparing(Destino::getId))
                .collect(Collectors.toList());
    }

    public Destino buscarPorId(Long id) {
        Destino destino = destinos.get(id);
        if (destino == null) {
            throw new DestinoNaoEncontradoException(id);
        }
        return destino;
    }

    public Destino atualizar(Long id, Destino dadosAtualizados) {
        Destino existente = buscarPorId(id);

        existente.setNome(dadosAtualizados.getNome());
        existente.setLocalizacao(dadosAtualizados.getLocalizacao());
        existente.setDescricao(dadosAtualizados.getDescricao());
        existente.setHoteisDisponiveis(dadosAtualizados.getHoteisDisponiveis());
        existente.setAtividadesTuristicas(dadosAtualizados.getAtividadesTuristicas());

        return existente;
    }

    public Destino registrarAvaliacao(Long id, int nota) {
        Destino destino = buscarPorId(id);
        destino.adicionarAvaliacao(nota);
        return destino;
    }

    public void excluir(Long id) {
        // o proprio remove ja diz se existia. buscar antes abre uma janela entre os dois.
        if (destinos.remove(id) == null) {
            throw new DestinoNaoEncontradoException(id);
        }
    }

    private void popularDadosIniciais() {
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
