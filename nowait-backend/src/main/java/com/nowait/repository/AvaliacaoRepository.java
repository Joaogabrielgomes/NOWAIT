package com.nowait.repository;

import com.nowait.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, String> {

    boolean existsByFilaId(String filaId);

    boolean existsByAgendamentoId(String agendamentoId);

    List<Avaliacao> findByEstabelecimentoIdOrderByCreatedAtDesc(String estabelecimentoId);

    long countByEstabelecimentoId(String estabelecimentoId);

    @Query("SELECT AVG(a.nota) FROM Avaliacao a WHERE a.estabelecimento.id = :estabelecimentoId")
    Double calcularMediaPorEstabelecimento(@Param("estabelecimentoId") String estabelecimentoId);
}
