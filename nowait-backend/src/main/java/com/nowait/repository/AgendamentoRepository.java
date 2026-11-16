package com.nowait.repository;

import com.nowait.model.Agendamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, String> {

    Page<Agendamento> findByEstabelecimentoId(String estabelecimentoId, Pageable pageable);

    Page<Agendamento> findByClienteId(String clienteId, Pageable pageable);

    List<Agendamento> findByStatusInAndDataHoraBefore(List<String> status, LocalDateTime referencia);

    List<Agendamento> findByEstabelecimentoIdAndStatusInAndDataHoraBetween(
            String estabelecimentoId, List<String> status, LocalDateTime inicio, LocalDateTime fim);

    long countByEstabelecimentoIdAndStatusInAndDataHora(
            String estabelecimentoId, List<String> status, LocalDateTime dataHora);

    List<Agendamento> findByClienteIdAndStatusIn(String clienteId, List<String> status);

    Optional<Agendamento> findFirstByMesaIdAndStatusAndAvaliacaoLiberadaFalseOrderByUpdatedAtDesc(
            String mesaId, String status);

    List<Agendamento> findByClienteIdAndStatusAndAvaliacaoLiberadaTrueOrderByUpdatedAtDesc(
            String clienteId, String status);

    boolean existsByClienteId(String clienteId);

    List<Agendamento> findByStatusAndMesaIsNull(String status);
}
