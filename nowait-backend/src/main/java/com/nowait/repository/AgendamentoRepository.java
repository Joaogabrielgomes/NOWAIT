package com.nowait.repository;

import com.nowait.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, String> {

    List<Agendamento> findByEstabelecimentoIdOrderByDataHoraAsc(String estabelecimentoId);

    List<Agendamento> findByClienteIdOrderByDataHoraDesc(String clienteId);

    List<Agendamento> findByStatusAndDataHoraBefore(String status, LocalDateTime referencia);

    List<Agendamento> findByStatusInAndDataHoraBefore(List<String> status, LocalDateTime referencia);

    Optional<Agendamento> findFirstByMesaIdAndStatusAndAvaliacaoLiberadaFalseOrderByUpdatedAtDesc(
            String mesaId, String status);

    List<Agendamento> findByClienteIdAndStatusAndAvaliacaoLiberadaTrueOrderByUpdatedAtDesc(
            String clienteId, String status);

    boolean existsByClienteId(String clienteId);
}
