package com.nowait.service;

import com.nowait.dto.AgendamentoRequestDTO;
import com.nowait.dto.AgendamentoResponseDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.exception.UnauthorizedOperationException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.Agendamento;
import com.nowait.model.Estabelecimento;
import com.nowait.model.Mesa;
import com.nowait.model.StatusAgendamento;
import com.nowait.model.StatusMesa;
import com.nowait.model.Usuario;
import com.nowait.repository.AgendamentoRepository;
import com.nowait.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AgendamentoService {

    private static final String ENTIDADE_AGENDAMENTO = "Agendamento";

    private final AgendamentoRepository  agendamentoRepository;
    private final MesaRepository         mesaRepository;
    private final EstabelecimentoService estabelecimentoService;
    private final AlocacaoMesaService    alocacaoMesaService;
    private final AuditoriaService       auditoriaService;

    @Value("${nowait.agendamento.tolerancia-padrao-minutos}")
    private int toleranciaPadraoMinutos;

    public AgendamentoResponseDTO criar(String estabelecimentoId, AgendamentoRequestDTO dto, Usuario cliente) {
        Estabelecimento estabelecimento = estabelecimentoService.buscarEntidadePorId(estabelecimentoId);

        if (!Boolean.TRUE.equals(estabelecimento.getAberto())) {
            throw new BusinessRuleException("Este estabelecimento não está aceitando agendamentos no momento");
        }

        Agendamento agendamento = Agendamento.builder()
                .estabelecimento(estabelecimento)
                .cliente(cliente)
                .quantidadePessoas(dto.getQuantidadePessoas())
                .dataHora(dto.getDataHora())
                .toleranciaMinutos(dto.getToleranciaMinutos() != null ? dto.getToleranciaMinutos() : toleranciaPadraoMinutos)
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        agendamento = agendamentoRepository.save(agendamento);
        log.info("Agendamento criado: id={} cliente={} estabelecimento={}",
                agendamento.getId(), cliente.getId(), estabelecimentoId);

        auditoriaService.registrar(cliente, AcaoAuditoria.AGENDAMENTO_CRIADO, ENTIDADE_AGENDAMENTO,
                agendamento.getId(), "data=" + agendamento.getDataHora());
        return toResponseDTO(agendamento);
    }

    public List<AgendamentoResponseDTO> listarPorEstabelecimento(String estabelecimentoId, Usuario solicitante) {
        Estabelecimento estabelecimento = estabelecimentoService.buscarEntidadePorId(estabelecimentoId);
        estabelecimentoService.validarDono(estabelecimento, solicitante);
        return agendamentoRepository.findByEstabelecimentoIdOrderByDataHoraAsc(estabelecimentoId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<AgendamentoResponseDTO> listarMeus(Usuario cliente) {
        return agendamentoRepository.findByClienteIdOrderByDataHoraDesc(cliente.getId()).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public AgendamentoResponseDTO cancelar(String agendamentoId, Usuario solicitante) {
        Agendamento agendamento = buscarEntidadePorId(agendamentoId);

        boolean ehCliente = agendamento.getCliente().getId().equals(solicitante.getId());
        boolean ehDono = agendamento.getEstabelecimento().getUsuario().getId().equals(solicitante.getId());
        if (!ehCliente && !ehDono) {
            throw new UnauthorizedOperationException("Você não tem permissão para cancelar este agendamento");
        }

        if (!StatusAgendamento.CONFIRMADO.equals(agendamento.getStatus())) {
            throw new BusinessRuleException("Somente agendamentos CONFIRMADOS podem ser cancelados");
        }

        liberarMesaSeNecessario(agendamento);
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento = agendamentoRepository.save(agendamento);

        auditoriaService.registrar(solicitante, AcaoAuditoria.AGENDAMENTO_CANCELADO, ENTIDADE_AGENDAMENTO,
                agendamento.getId(), null);
        return toResponseDTO(agendamento);
    }

    public AgendamentoResponseDTO confirmarChegada(String agendamentoId, Usuario solicitante) {
        Agendamento agendamento = buscarEntidadePorId(agendamentoId);
        estabelecimentoService.validarDono(agendamento.getEstabelecimento(), solicitante);

        if (!StatusAgendamento.CONFIRMADO.equals(agendamento.getStatus())) {
            throw new BusinessRuleException("Somente agendamentos CONFIRMADOS podem ter a chegada confirmada");
        }

        Mesa mesa = alocacaoMesaService
                .alocarMesa(agendamento.getEstabelecimento().getId(), agendamento.getQuantidadePessoas())
                .orElseThrow(() -> new BusinessRuleException(
                        "Nenhuma mesa disponível com capacidade suficiente no momento"));

        mesa.setStatus(StatusMesa.OCUPADA);
        mesaRepository.save(mesa);

        agendamento.setMesa(mesa);
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        agendamento = agendamentoRepository.save(agendamento);

        auditoriaService.registrar(solicitante, AcaoAuditoria.AGENDAMENTO_CONFIRMADO, ENTIDADE_AGENDAMENTO,
                agendamento.getId(), "mesa=" + mesa.getId());
        return toResponseDTO(agendamento);
    }

    @Scheduled(fixedDelay = 60_000)
    public void expirarAgendamentosVencidos() {
        LocalDateTime agora = LocalDateTime.now();
        List<Agendamento> candidatos = agendamentoRepository.findByStatusAndDataHoraBefore(StatusAgendamento.CONFIRMADO, agora);

        for (Agendamento agendamento : candidatos) {
            LocalDateTime limite = agendamento.getDataHora().plusMinutes(agendamento.getToleranciaMinutos());
            if (limite.isBefore(agora)) {
                liberarMesaSeNecessario(agendamento);
                agendamento.setStatus(StatusAgendamento.EXPIRADO);
                agendamentoRepository.save(agendamento);
                log.info("Agendamento {} expirado por tolerância", agendamento.getId());
                auditoriaService.registrar(agendamento.getCliente(), AcaoAuditoria.AGENDAMENTO_EXPIRADO,
                        ENTIDADE_AGENDAMENTO, agendamento.getId(), "expirado automaticamente por tolerância");
            }
        }
    }

    private void liberarMesaSeNecessario(Agendamento agendamento) {
        if (agendamento.getMesa() != null) {
            Mesa mesa = agendamento.getMesa();
            mesa.setStatus(StatusMesa.LIVRE);
            mesaRepository.save(mesa);
        }
    }

    private Agendamento buscarEntidadePorId(String id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento", "id", id));
    }

    private AgendamentoResponseDTO toResponseDTO(Agendamento agendamento) {
        return AgendamentoResponseDTO.builder()
                .id(agendamento.getId())
                .estabelecimentoId(agendamento.getEstabelecimento().getId())
                .estabelecimentoNome(agendamento.getEstabelecimento().getNome())
                .clienteId(agendamento.getCliente().getId())
                .clienteNome(agendamento.getCliente().getNome())
                .mesaId(agendamento.getMesa() != null ? agendamento.getMesa().getId() : null)
                .quantidadePessoas(agendamento.getQuantidadePessoas())
                .dataHora(agendamento.getDataHora())
                .toleranciaMinutos(agendamento.getToleranciaMinutos())
                .status(agendamento.getStatus())
                .createdAt(agendamento.getCreatedAt())
                .build();
    }
}
