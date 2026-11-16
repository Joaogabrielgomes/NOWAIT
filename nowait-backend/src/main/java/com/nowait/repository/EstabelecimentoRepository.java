package com.nowait.repository;

import com.nowait.model.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EstabelecimentoRepository
        extends JpaRepository<Estabelecimento, String>, JpaSpecificationExecutor<Estabelecimento> {

    Optional<Estabelecimento> findByUsuarioId(String usuarioId);

    boolean existsByUsuarioId(String usuarioId);

    Optional<Estabelecimento> findByTokenContestacaoRejeicaoAndStatusAprovacao(
            String tokenContestacaoRejeicao, String statusAprovacao);

    long countByAberto(Boolean aberto);

    @Query(value =
            "SELECT DATE(created_at) AS dia, COUNT(*) AS quantidade " +
            "FROM estabelecimentos " +
            "WHERE created_at >= :desde " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY dia ASC",
            nativeQuery = true)
    List<ContagemPorDiaProjection> contarNovosPorDia(@Param("desde") LocalDateTime desde);
}
