package br.com.senai.agenciaviagens.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AvaliacaoRequest {

    @NotNull(message = "A nota e obrigatoria")
    @Min(value = 1, message = "A nota minima e 1")
    @Max(value = 5, message = "A nota maxima e 5")
    private Integer nota;

    public AvaliacaoRequest() {
    }

    public AvaliacaoRequest(Integer nota) {
        this.nota = nota;
    }

    public Integer getNota() {
        return nota;
    }

    public void setNota(Integer nota) {
        this.nota = nota;
    }
}
