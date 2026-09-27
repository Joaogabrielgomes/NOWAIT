package com.nowait.repository;

import com.nowait.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, String> {

    List<Agendamento> findByEstabelecimentoIdOrderByDataHoraAsc(String estabelecimentoId);

    List<Agendamento> findByClienteIdOrderByDataHoraDesc(String clienteId);

    List<Agendamento> findByStatusAndDataHoraBefore(String status, LocalDateTime referencia);

    boolean existsByClienteId(String clienteId);
}
