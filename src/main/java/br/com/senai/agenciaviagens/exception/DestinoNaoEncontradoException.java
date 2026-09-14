package br.com.senai.agenciaviagens.exception;

public class DestinoNaoEncontradoException extends RuntimeException {

    public DestinoNaoEncontradoException(Long id) {
        super("Destino com id " + id + " não foi encontrado");
    }
}
