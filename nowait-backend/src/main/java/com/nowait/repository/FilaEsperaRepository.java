package com.nowait.repository;

import com.nowait.model.FilaEspera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FilaEsperaRepository extends JpaRepository<FilaEspera, String> {

    List<FilaEspera> findByEstabelecimentoIdAndStatusOrderByHoraEntradaAsc(String estabelecimentoId, String status);

    List<FilaEspera> findByEstabelecimentoIdOrderByHoraEntradaAsc(String estabelecimentoId);

    Optional<FilaEspera> findByClienteIdAndEstabelecimentoIdAndStatusIn(
            String clienteId, String estabelecimentoId, List<String> status);

    Optional<FilaEspera> findFirstByClienteIdAndStatusInOrderByHoraEntradaDesc(
            String clienteId, List<String> status);

    List<FilaEspera> findByClienteIdOrderByHoraEntradaDesc(String clienteId);

    Optional<FilaEspera> findFirstByMesaIdAndStatusAndAvaliacaoLiberadaFalseOrderByHoraAtendimentoDesc(
            String mesaId, String status);

    List<FilaEspera> findByClienteIdAndStatusAndAvaliacaoLiberadaTrueOrderByHoraAtendimentoDesc(
            String clienteId, String status);

    boolean existsByClienteId(String clienteId);

    long countByEstabelecimentoIdAndStatusAndHoraEntradaBefore(
            String estabelecimentoId, String status, java.time.LocalDateTime horaEntrada);

    @Query("SELECT COALESCE(SUM(f.quantidadePessoas), 0) FROM FilaEspera f WHERE f.status IN :status")
    long somarPessoasPorStatus(@Param("status") List<String> status);
}
