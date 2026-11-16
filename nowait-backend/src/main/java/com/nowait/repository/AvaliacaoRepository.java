package com.nowait.repository;

import com.nowait.model.Avaliacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, String> {

    boolean existsByFilaId(String filaId);

    boolean existsByAgendamentoId(String agendamentoId);

    Page<Avaliacao> findByEstabelecimentoId(String estabelecimentoId, Pageable pageable);

    Page<Avaliacao> findByClienteId(String clienteId, Pageable pageable);

    long countByEstabelecimentoId(String estabelecimentoId);

    @Query("SELECT AVG(a.nota) FROM Avaliacao a WHERE a.estabelecimento.id = :estabelecimentoId")
    Double calcularMediaPorEstabelecimento(@Param("estabelecimentoId") String estabelecimentoId);
}
