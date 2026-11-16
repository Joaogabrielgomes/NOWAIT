package com.nowait.service;

import com.nowait.dto.AgendamentoMotivoDTO;
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
import com.nowait.model.StatusAprovacaoEstabelecimento;
import com.nowait.model.StatusMesa;
import com.nowait.model.Usuario;
import com.nowait.repository.AgendamentoRepository;
import com.nowait.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AgendamentoService {

    private static final String ENTIDADE_AGENDAMENTO = "Agendamento";
    private static final List<String> STATUS_CANCELAVEIS = List.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO);
    private static final List<String> STATUS_EXPIRAVEIS = List.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO);
    private static final List<String> STATUS_CONSIDERADOS_NA_CRIACAO =
            List.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO);
    private static final List<String> STATUS_CONSIDERADOS_NO_ACEITE = List.of(StatusAgendamento.CONFIRMADO);
    private static final int MINUTOS_INTERVALO_SLOT = 15;

    private final AgendamentoRepository  agendamentoRepository;
    private final MesaRepository         mesaRepository;
    private final EstabelecimentoService estabelecimentoService;
    private final AlocacaoMesaService    alocacaoMesaService;
    private final VerificadorCapacidadeAgendamento verificadorCapacidade;
    private final AuditoriaService       auditoriaService;
    private final EmailService           emailService;

    public AgendamentoResponseDTO criar(String estabelecimentoId, AgendamentoRequestDTO dto, Usuario cliente) {
        Estabelecimento estabelecimento = estabelecimentoService.buscarEntidadePorId(estabelecimentoId);

        if (!StatusAprovacaoEstabelecimento.APROVADO.equals(estabelecimento.getStatusAprovacao())) {
            throw new BusinessRuleException("Este estabelecimento ainda não está disponível");
        }

        if (!Boolean.TRUE.equals(estabelecimento.getAberto())) {
            throw new BusinessRuleException("Este estabelecimento não está aceitando agendamentos no momento");
        }

        int antecedenciaMinimaMinutos = estabelecimento.getAntecedenciaMinimaMinutos();
        if (dto.getDataHora().isBefore(LocalDateTime.now().plusMinutes(antecedenciaMinimaMinutos))) {
            throw new BusinessRuleException(
                    "O agendamento deve ser feito com no mínimo " + antecedenciaMinimaMinutos + " minutos de antecedência");
        }

        validarAntecedenciaMaxima(estabelecimento, dto.getDataHora());
        validarSlotDeQuinzeMinutos(dto.getDataHora());
        validarDentroDoHorarioFuncionamento(estabelecimento, dto.getDataHora());
        validarSemConflitoEmOutroEstabelecimento(cliente, estabelecimentoId, dto.getDataHora());

        verificadorCapacidade.validar(estabelecimento, dto.getDataHora(), dto.getQuantidadePessoas(),
                STATUS_CONSIDERADOS_NA_CRIACAO);
        verificadorCapacidade.validarLimitePorHorario(estabelecimento, dto.getDataHora(), STATUS_CONSIDERADOS_NA_CRIACAO);

        Agendamento agendamento = Agendamento.builder()
                .estabelecimento(estabelecimento)
                .cliente(cliente)
                .quantidadePessoas(dto.getQuantidadePessoas())
                .dataHora(dto.getDataHora())
                .toleranciaMinutos(dto.getToleranciaMinutos() != null
                        ? dto.getToleranciaMinutos() : estabelecimento.getToleranciaPadraoMinutos())
                .status(StatusAgendamento.PENDENTE)
                .build();

        agendamento = agendamentoRepository.save(agendamento);
        final Agendamento agendamentoCriado = agendamento;
        log.info("Agendamento criado: id={} cliente={} estabelecimento={}",
                agendamentoCriado.getId(), cliente.getId(), estabelecimentoId);

        auditoriaService.registrar(cliente, AcaoAuditoria.AGENDAMENTO_CRIADO, ENTIDADE_AGENDAMENTO,
                agendamentoCriado.getId(), "data=" + agendamentoCriado.getDataHora());

        emailService.enviarComSeguranca(() -> emailService.enviarEmailAgendamentoCriado(
                agendamentoCriado.getEstabelecimento().getUsuario().getEmail(), cliente.getNome(),
                agendamentoCriado.getDataHora(), agendamentoCriado.getQuantidadePessoas()));
        return toResponseDTO(agendamentoCriado);
    }

    private void validarAntecedenciaMaxima(Estabelecimento estabelecimento, LocalDateTime dataHora) {
        LocalDateTime limiteMaximo = LocalDateTime.now().plusDays(estabelecimento.getAntecedenciaMaximaDias());
        if (dataHora.isAfter(limiteMaximo)) {
            throw new BusinessRuleException(
                    "Este estabelecimento só aceita agendamentos com até "
                            + estabelecimento.getAntecedenciaMaximaDias() + " dias de antecedência");
        }
    }

    private void validarSlotDeQuinzeMinutos(LocalDateTime dataHora) {
        boolean minutoValido = dataHora.getMinute() % MINUTOS_INTERVALO_SLOT == 0;
        boolean semSegundos = dataHora.getSecond() == 0 && dataHora.getNano() == 0;
        if (!minutoValido || !semSegundos) {
            throw new BusinessRuleException(
                    "O horário do agendamento deve ser em intervalos de " + MINUTOS_INTERVALO_SLOT + " minutos");
        }
    }

    private void validarDentroDoHorarioFuncionamento(Estabelecimento estabelecimento, LocalDateTime dataHora) {
        LocalTime horarioSolicitado = dataHora.toLocalTime();
        LocalTime ultimoHorarioPermitido = estabelecimento.getHorarioFechamento()
                .minusMinutes(estabelecimento.getTempoMedioAtendimentoMinutos());

        if (horarioSolicitado.isBefore(estabelecimento.getHorarioAbertura())
                || horarioSolicitado.isAfter(ultimoHorarioPermitido)) {
            throw new BusinessRuleException(
                    "O agendamento deve estar dentro do horário de funcionamento do estabelecimento ("
                            + estabelecimento.getHorarioAbertura() + " às " + estabelecimento.getHorarioFechamento()
                            + "), respeitando uma antecedência mínima de "
                            + estabelecimento.getTempoMedioAtendimentoMinutos() + " minutos antes do fechamento");
        }
    }

    private void validarSemConflitoEmOutroEstabelecimento(Usuario cliente, String estabelecimentoId, LocalDateTime dataHora) {
        boolean temConflito = agendamentoRepository
                .findByClienteIdAndStatusIn(cliente.getId(), STATUS_CONSIDERADOS_NA_CRIACAO)
                .stream()
                .filter(outro -> !outro.getEstabelecimento().getId().equals(estabelecimentoId))
                .anyMatch(outro -> seSobrepoe(outro, dataHora));

        if (temConflito) {
            throw new BusinessRuleException(
                    "Você já possui um agendamento em outro estabelecimento próximo deste horário");
        }
    }

    private boolean seSobrepoe(Agendamento existente, LocalDateTime novaDataHora) {
        int duracaoMinutos = existente.getEstabelecimento().getTempoMedioAtendimentoMinutos();
        long diferencaMinutos = Math.abs(Duration.between(existente.getDataHora(), novaDataHora).toMinutes());
        return diferencaMinutos < duracaoMinutos;
    }

    public Page<AgendamentoResponseDTO> listarPorEstabelecimento(
            String estabelecimentoId, Usuario solicitante, int pagina, int tamanho) {
        Estabelecimento estabelecimento = estabelecimentoService.buscarEntidadePorId(estabelecimentoId);
        estabelecimentoService.validarDono(estabelecimento, solicitante);
        PageRequest paginacao = PageRequest.of(pagina, tamanho, Sort.by("dataHora").ascending());
        return agendamentoRepository.findByEstabelecimentoId(estabelecimentoId, paginacao)
                .map(this::toResponseDTO);
    }

    public Page<AgendamentoResponseDTO> listarMeus(Usuario cliente, int pagina, int tamanho) {
        PageRequest paginacao = PageRequest.of(pagina, tamanho, Sort.by("dataHora").descending());
        return agendamentoRepository.findByClienteId(cliente.getId(), paginacao)
                .map(this::toResponseDTO);
    }

    public AgendamentoResponseDTO cancelar(String agendamentoId, AgendamentoMotivoDTO dto, Usuario solicitante) {
        Agendamento agendamento = buscarEntidadePorId(agendamentoId);

        boolean ehCliente = agendamento.getCliente().getId().equals(solicitante.getId());
        boolean ehDono = agendamento.getEstabelecimento().getUsuario().getId().equals(solicitante.getId());
        if (!ehCliente && !ehDono) {
            throw new UnauthorizedOperationException("Você não tem permissão para cancelar este agendamento");
        }

        if (!STATUS_CANCELAVEIS.contains(agendamento.getStatus())) {
            throw new BusinessRuleException("Este agendamento não pode mais ser cancelado");
        }

        String motivo = null;
        if (ehDono) {
            motivo = extrairMotivoObrigatorio(dto, "Informe o motivo do cancelamento");
        }

        liberarMesaSeNecessario(agendamento);
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        if (motivo != null) {
            agendamento.setMotivoCancelamento(motivo);
        }
        agendamento = agendamentoRepository.save(agendamento);
        final Agendamento agendamentoCancelado = agendamento;
        final String motivoParaEmail = motivo;

        auditoriaService.registrar(solicitante, AcaoAuditoria.AGENDAMENTO_CANCELADO, ENTIDADE_AGENDAMENTO,
                agendamentoCancelado.getId(), motivoParaEmail != null ? "motivo=" + motivoParaEmail : null);

        if (ehDono) {
            emailService.enviarComSeguranca(() -> emailService.enviarEmailAgendamentoCancelado(
                    agendamentoCancelado.getCliente().getEmail(), agendamentoCancelado.getEstabelecimento().getNome(),
                    agendamentoCancelado.getEstabelecimento().getTelefone(), agendamentoCancelado.getDataHora(),
                    motivoParaEmail));
        } else {
            emailService.enviarComSeguranca(() -> emailService.enviarEmailAgendamentoCanceladoPeloCliente(
                    agendamentoCancelado.getEstabelecimento().getUsuario().getEmail(),
                    agendamentoCancelado.getCliente().getNome(), agendamentoCancelado.getDataHora()));
        }
        return toResponseDTO(agendamentoCancelado);
    }

    public AgendamentoResponseDTO aceitar(String agendamentoId, Usuario solicitante) {
        Agendamento agendamento = buscarEntidadePorId(agendamentoId);
        estabelecimentoService.validarDono(agendamento.getEstabelecimento(), solicitante);

        if (!StatusAgendamento.PENDENTE.equals(agendamento.getStatus())) {
            throw new BusinessRuleException("Somente agendamentos PENDENTES podem ser aceitos");
        }

        verificadorCapacidade.validar(agendamento.getEstabelecimento(), agendamento.getDataHora(),
                agendamento.getQuantidadePessoas(), STATUS_CONSIDERADOS_NO_ACEITE);
        verificadorCapacidade.validarLimitePorHorario(agendamento.getEstabelecimento(), agendamento.getDataHora(),
                STATUS_CONSIDERADOS_NO_ACEITE);

        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamento = agendamentoRepository.save(agendamento);
        final Agendamento agendamentoConfirmado = agendamento;

        auditoriaService.registrar(solicitante, AcaoAuditoria.AGENDAMENTO_ACEITO, ENTIDADE_AGENDAMENTO,
                agendamentoConfirmado.getId(), null);

        emailService.enviarComSeguranca(() -> emailService.enviarEmailAgendamentoAceito(
                agendamentoConfirmado.getCliente().getEmail(), agendamentoConfirmado.getEstabelecimento().getNome(),
                agendamentoConfirmado.getEstabelecimento().getTelefone(), agendamentoConfirmado.getDataHora()));
        return toResponseDTO(agendamentoConfirmado);
    }

    public AgendamentoResponseDTO recusar(String agendamentoId, AgendamentoMotivoDTO dto, Usuario solicitante) {
        Agendamento agendamento = buscarEntidadePorId(agendamentoId);
        estabelecimentoService.validarDono(agendamento.getEstabelecimento(), solicitante);

        if (!StatusAgendamento.PENDENTE.equals(agendamento.getStatus())) {
            throw new BusinessRuleException("Somente agendamentos PENDENTES podem ser recusados");
        }

        String motivo = extrairMotivoObrigatorio(dto, "Informe o motivo da recusa");

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setMotivoCancelamento(motivo);
        agendamento = agendamentoRepository.save(agendamento);
        final Agendamento agendamentoRecusado = agendamento;

        auditoriaService.registrar(solicitante, AcaoAuditoria.AGENDAMENTO_RECUSADO, ENTIDADE_AGENDAMENTO,
                agendamentoRecusado.getId(), "motivo=" + motivo);

        emailService.enviarComSeguranca(() -> emailService.enviarEmailAgendamentoRecusado(
                agendamentoRecusado.getCliente().getEmail(), agendamentoRecusado.getEstabelecimento().getNome(),
                agendamentoRecusado.getEstabelecimento().getTelefone(), agendamentoRecusado.getDataHora(), motivo));
        return toResponseDTO(agendamentoRecusado);
    }

    private String extrairMotivoObrigatorio(AgendamentoMotivoDTO dto, String mensagemErro) {
        String motivo = dto != null ? dto.getMotivo() : null;
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessRuleException(mensagemErro);
        }
        return motivo.trim();
    }

    public AgendamentoResponseDTO confirmarChegada(String agendamentoId, Usuario solicitante) {
        Agendamento agendamento = buscarEntidadePorId(agendamentoId);
        estabelecimentoService.validarDono(agendamento.getEstabelecimento(), solicitante);

        if (!StatusAgendamento.CONFIRMADO.equals(agendamento.getStatus())) {
            throw new BusinessRuleException("Somente agendamentos CONFIRMADOS podem ter a chegada confirmada");
        }

        Mesa mesa;
        if (agendamento.getMesa() != null) {
            mesa = agendamento.getMesa();
            mesa.setStatus(StatusMesa.OCUPADA);
            mesaRepository.save(mesa);
        } else {
            mesa = alocacaoMesaService
                    .alocarMesa(agendamento.getEstabelecimento().getId(), agendamento.getQuantidadePessoas(), StatusMesa.OCUPADA)
                    .orElseThrow(() -> new BusinessRuleException(
                            "Nenhuma mesa disponível com capacidade suficiente no momento"));
        }

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
        List<Agendamento> candidatos = agendamentoRepository.findByStatusInAndDataHoraBefore(STATUS_EXPIRAVEIS, agora);

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

    @Scheduled(fixedDelay = 60_000)
    public void reservarMesasProximasDoHorario() {
        LocalDateTime agora = LocalDateTime.now();
        List<Agendamento> candidatos = agendamentoRepository.findByStatusAndMesaIsNull(StatusAgendamento.CONFIRMADO);

        for (Agendamento agendamento : candidatos) {
            Estabelecimento estabelecimento = agendamento.getEstabelecimento();
            LocalDateTime momentoReserva = agendamento.getDataHora()
                    .minusMinutes(estabelecimento.getAntecedenciaReservaMesaMinutos());

            if (agora.isBefore(momentoReserva)) {
                continue;
            }

            alocacaoMesaService.alocarMesa(estabelecimento.getId(), agendamento.getQuantidadePessoas(), StatusMesa.RESERVADA)
                    .ifPresent(mesa -> {
                        agendamento.setMesa(mesa);
                        agendamentoRepository.save(agendamento);
                        log.info("Mesa {} pré-reservada para agendamento {}", mesa.getId(), agendamento.getId());
                    });
        }
    }

    private void liberarMesaSeNecessario(Agendamento agendamento) {
        if (agendamento.getMesa() != null) {
            Mesa mesa = agendamento.getMesa();
            mesa.setStatus(StatusMesa.LIVRE);
            mesaRepository.save(mesa);
        }
    }

    public void marcarAvaliacaoLiberadaPorMesa(String mesaId) {
        agendamentoRepository
                .findFirstByMesaIdAndStatusAndAvaliacaoLiberadaFalseOrderByUpdatedAtDesc(mesaId, StatusAgendamento.CONCLUIDO)
                .ifPresent(agendamento -> {
                    agendamento.setAvaliacaoLiberada(true);
                    agendamentoRepository.save(agendamento);
                });
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
                .motivoCancelamento(agendamento.getMotivoCancelamento())
                .avaliacaoLiberada(agendamento.getAvaliacaoLiberada())
                .createdAt(agendamento.getCreatedAt())
                .build();
    }
}
