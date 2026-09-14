package br.com.senai.agenciaviagens.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Destino {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "O nome do destino e obrigatorio")
    private String nome;

    @NotBlank(message = "A localizacao do destino e obrigatoria")
    private String localizacao;

    private String descricao;

    @PositiveOrZero(message = "A quantidade de hoteis nao pode ser negativa")
    private Integer hoteisDisponiveis = 0;

    private List<String> atividadesTuristicas = new ArrayList<>();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final List<Integer> avaliacoes = new CopyOnWriteArrayList<>();

    public Destino() {
    }

    public Destino(Long id, String nome, String localizacao, String descricao,
                   Integer hoteisDisponiveis, List<String> atividadesTuristicas) {
        this.id = id;
        this.nome = nome;
        this.localizacao = localizacao;
        this.descricao = descricao;
        setHoteisDisponiveis(hoteisDisponiveis);
        setAtividadesTuristicas(atividadesTuristicas);
    }

    public void adicionarAvaliacao(int nota) {
        if (nota < 1 || nota > 5) {
            throw new IllegalArgumentException("A nota deve estar entre 1 e 5");
        }
        this.avaliacoes.add(nota);
    }

    public double getMediaAvaliacoes() {
        if (avaliacoes.isEmpty()) {
            return 0.0;
        }
        double soma = 0;
        for (int nota : avaliacoes) {
            soma += nota;
        }
        return Math.round((soma / avaliacoes.size()) * 10.0) / 10.0;
    }

    public int getQuantidadeAvaliacoes() {
        return avaliacoes.size();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getHoteisDisponiveis() {
        return hoteisDisponiveis;
    }

    public void setHoteisDisponiveis(Integer hoteisDisponiveis) {
        this.hoteisDisponiveis = hoteisDisponiveis != null ? hoteisDisponiveis : 0;
    }

    public List<String> getAtividadesTuristicas() {
        return List.copyOf(atividadesTuristicas);
    }

    public void setAtividadesTuristicas(List<String> atividadesTuristicas) {
        this.atividadesTuristicas = atividadesTuristicas != null
                ? new ArrayList<>(atividadesTuristicas)
                : new ArrayList<>();
    }

    public List<Integer> getAvaliacoes() {
        return List.copyOf(avaliacoes);
    }
}
