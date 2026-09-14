package br.com.senai.agenciaviagens.repository;

import br.com.senai.agenciaviagens.model.Destino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DestinoRepository extends JpaRepository<Destino, Long> {

}
