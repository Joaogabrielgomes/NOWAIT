package com.nowait.repository;

import com.nowait.model.AcaoAuditoria;
import com.nowait.model.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, String> {

    @Query("SELECT l FROM LogAuditoria l WHERE " +
           "(:usuarioId IS NULL OR l.usuarioId = :usuarioId) AND " +
           "(:acao IS NULL OR l.acao = :acao) " +
           "ORDER BY l.criadoEm DESC")
    Page<LogAuditoria> buscarComFiltros(@Param("usuarioId") String usuarioId,
                                         @Param("acao") AcaoAuditoria acao,
                                         Pageable pageable);

    @Query("SELECT l FROM LogAuditoria l WHERE " +
           "(:usuarioId IS NULL OR l.usuarioId = :usuarioId) AND " +
           "(:acao IS NULL OR l.acao = :acao) " +
           "ORDER BY l.criadoEm DESC")
    List<LogAuditoria> buscarTodosComFiltros(@Param("usuarioId") String usuarioId,
                                              @Param("acao") AcaoAuditoria acao);

    long deleteByCriadoEmBefore(LocalDateTime limite);

    List<LogAuditoria> findTop20ByOrderByCriadoEmDesc();

    @Query(value =
            "SELECT DATE(l.criado_em) AS dia, l.acao AS acao, COUNT(*) AS quantidade " +
            "FROM logs_auditoria l " +
            "WHERE l.criado_em >= :desde " +
            "GROUP BY DATE(l.criado_em), l.acao " +
            "ORDER BY dia ASC",
            nativeQuery = true)
    List<AcaoPorDiaProjection> contarAcoesPorDia(@Param("desde") LocalDateTime desde);
}
