package com.nowait.service;

import com.nowait.dto.FilaEsperaRequestDTO;
import com.nowait.dto.FilaEsperaResponseDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.exception.UnauthorizedOperationException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.Estabelecimento;
import com.nowait.model.FilaEspera;
import com.nowait.model.Mesa;
import com.nowait.model.StatusFila;
import com.nowait.model.StatusMesa;
import com.nowait.model.Usuario;
import com.nowait.repository.FilaEsperaRepository;
import com.nowait.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FilaEsperaService {

    private static final List<String> STATUS_ATIVOS = List.of(StatusFila.AGUARDANDO, StatusFila.CHAMADO);
    private static final String ENTIDADE_FILA = "FilaEspera";

    private final FilaEsperaRepository   filaEsperaRepository;
    private final MesaRepository         mesaRepository;
    private final EstabelecimentoService estabelecimentoService;
    private final AlocacaoMesaService    alocacaoMesaService;
    private final SimpMessagingTemplate  messagingTemplate;
    private final AuditoriaService       auditoriaService;

    public FilaEsperaResponseDTO entrarNaFila(String estabelecimentoId, FilaEsperaRequestDTO dto, Usuario cliente) {
        Estabelecimento estabelecimento = estabelecimentoService.buscarEntidadePorId(estabelecimentoId);
        validarPodeEntrarNaFila(estabelecimento, cliente, estabelecimentoId);

        FilaEspera entrada = salvarNovaEntrada(estabelecimento, cliente, dto.getQuantidadePessoas());

        auditoriaService.registrar(cliente, AcaoAuditoria.FILA_ENTRADA, ENTIDADE_FILA, entrada.getId(),
                "estabelecimento=" + estabelecimentoId + ", pessoas=" + dto.getQuantidadePessoas());
        notificarFila(estabelecimentoId);
        return toResponseDTO(entrada);
    }

    private void validarPodeEntrarNaFila(Estabelecimento estabelecimento, Usuario cliente, String estabelecimentoId) {
        if (!Boolean.TRUE.equals(estabelecimento.getAberto())) {
            throw new BusinessRuleException("Este estabelecimento não está aceitando entradas na fila no momento");
        }

        filaEsperaRepository.findByClienteIdAndEstabelecimentoIdAndStatusIn(
                cliente.getId(), estabelecimentoId, STATUS_ATIVOS
        ).ifPresent(f -> {
            throw new BusinessRuleException("Você já possui uma posição ativa na fila deste estabelecimento");
        });
    }

    private FilaEspera salvarNovaEntrada(Estabelecimento estabelecimento, Usuario cliente, Integer quantidadePessoas) {
        FilaEspera entrada = FilaEspera.builder()
                .estabelecimento(estabelecimento)
                .cliente(cliente)
                .quantidadePessoas(quantidadePessoas)
                .status(StatusFila.AGUARDANDO)
                .horaEntrada(LocalDateTime.now())
                .build();

        entrada = filaEsperaRepository.save(entrada);
        log.info("Cliente {} entrou na fila do estabelecimento {}", cliente.getId(), estabelecimento.getId());
        return entrada;
    }

    public List<FilaEsperaResponseDTO> listarFila(String estabelecimentoId, Usuario solicitante) {
        Estabelecimento estabelecimento = estabelecimentoService.buscarEntidadePorId(estabelecimentoId);
        estabelecimentoService.validarDono(estabelecimento, solicitante);
        return montarListaComPosicao(estabelecimentoId);
    }

    public Optional<FilaEsperaResponseDTO> minhaEntradaAtiva(Usuario cliente) {
        return filaEsperaRepository
                .findFirstByClienteIdAndStatusInOrderByHoraEntradaDesc(cliente.getId(), STATUS_ATIVOS)
                .map(entrada -> {
                    List<FilaEsperaResponseDTO> filaCompleta = montarListaComPosicao(entrada.getEstabelecimento().getId());
                    return filaCompleta.stream()
                            .filter(f -> f.getId().equals(entrada.getId()))
                            .findFirst()
                            .orElse(toResponseDTO(entrada));
                });
    }

    public FilaEsperaResponseDTO chamar(String filaId, Usuario solicitante) {
        FilaEspera entrada = buscarEntidadePorId(filaId);
        estabelecimentoService.validarDono(entrada.getEstabelecimento(), solicitante);

        if (!StatusFila.AGUARDANDO.equals(entrada.getStatus())) {
            throw new BusinessRuleException("Somente entradas com status AGUARDANDO podem ser chamadas");
        }

        Mesa mesa = alocacaoMesaService
                .alocarMesa(entrada.getEstabelecimento().getId(), entrada.getQuantidadePessoas())
                .orElseThrow(() -> new BusinessRuleException(
                        "Nenhuma mesa disponível com capacidade suficiente no momento"));

        efetivarChamada(entrada, mesa, solicitante, AcaoAuditoria.FILA_CHAMADA);
        log.info("Fila {} chamada manualmente, mesa {} alocada", filaId, mesa.getId());
        return toResponseDTO(entrada);
    }

    public void tentarChamarProximoAutomaticamente(String estabelecimentoId) {
        List<FilaEspera> aguardando =
                filaEsperaRepository.findByEstabelecimentoIdAndStatusOrderByHoraEntradaAsc(estabelecimentoId, StatusFila.AGUARDANDO);

        if (aguardando.isEmpty()) {
            return;
        }

        FilaEspera proxima = aguardando.get(0);
        alocacaoMesaService.alocarMesa(estabelecimentoId, proxima.getQuantidadePessoas())
                .ifPresent(mesa -> {
                    efetivarChamada(proxima, mesa, null, AcaoAuditoria.FILA_CHAMADA_AUTOMATICA);
                    log.info("Fila {} chamada automaticamente, mesa {} alocada", proxima.getId(), mesa.getId());
                });
    }

    private void efetivarChamada(FilaEspera entrada, Mesa mesa, Usuario responsavel, AcaoAuditoria acao) {
        mesa.setStatus(StatusMesa.OCUPADA);
        mesaRepository.save(mesa);

        entrada.setMesa(mesa);
        entrada.setStatus(StatusFila.CHAMADO);
        entrada.setHoraChamado(LocalDateTime.now());
        filaEsperaRepository.save(entrada);

        auditoriaService.registrar(responsavel, acao, ENTIDADE_FILA, entrada.getId(), "mesa=" + mesa.getId());
        notificarFila(entrada.getEstabelecimento().getId());
    }

    public FilaEsperaResponseDTO atender(String filaId, Usuario solicitante) {
        FilaEspera entrada = buscarEntidadePorId(filaId);
        estabelecimentoService.validarDono(entrada.getEstabelecimento(), solicitante);

        if (!StatusFila.CHAMADO.equals(entrada.getStatus())) {
            throw new BusinessRuleException("Somente entradas com status CHAMADO podem ser marcadas como atendidas");
        }

        entrada.setStatus(StatusFila.ATENDIDO);
        entrada.setHoraAtendimento(LocalDateTime.now());
        entrada = filaEsperaRepository.save(entrada);

        auditoriaService.registrar(solicitante, AcaoAuditoria.FILA_ATENDIMENTO, ENTIDADE_FILA, entrada.getId(), null);
        notificarFila(entrada.getEstabelecimento().getId());
        return toResponseDTO(entrada);
    }

    public FilaEsperaResponseDTO cancelar(String filaId, Usuario solicitante) {
        FilaEspera entrada = buscarEntidadePorId(filaId);

        boolean ehCliente = entrada.getCliente().getId().equals(solicitante.getId());
        boolean ehDono = entrada.getEstabelecimento().getUsuario().getId().equals(solicitante.getId());
        if (!ehCliente && !ehDono) {
            throw new UnauthorizedOperationException("Você não tem permissão para cancelar esta entrada na fila");
        }

        if (!STATUS_ATIVOS.contains(entrada.getStatus())) {
            throw new BusinessRuleException("Esta entrada na fila não pode mais ser cancelada");
        }

        if (entrada.getMesa() != null) {
            Mesa mesa = entrada.getMesa();
            mesa.setStatus(StatusMesa.LIVRE);
            mesaRepository.save(mesa);
        }

        entrada.setStatus(StatusFila.CANCELADO);
        entrada = filaEsperaRepository.save(entrada);

        auditoriaService.registrar(solicitante, AcaoAuditoria.FILA_CANCELAMENTO, ENTIDADE_FILA, entrada.getId(), null);
        notificarFila(entrada.getEstabelecimento().getId());
        return toResponseDTO(entrada);
    }

    private FilaEspera buscarEntidadePorId(String id) {
        return filaEsperaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada na fila", "id", id));
    }

    private void notificarFila(String estabelecimentoId) {
        messagingTemplate.convertAndSend(
                "/topic/fila." + estabelecimentoId,
                montarListaComPosicao(estabelecimentoId));
    }

    private List<FilaEsperaResponseDTO> montarListaComPosicao(String estabelecimentoId) {
        Estabelecimento estabelecimento = estabelecimentoService.buscarEntidadePorId(estabelecimentoId);
        List<FilaEspera> aguardando =
                filaEsperaRepository.findByEstabelecimentoIdAndStatusOrderByHoraEntradaAsc(estabelecimentoId, StatusFila.AGUARDANDO);
        List<FilaEspera> chamados =
                filaEsperaRepository.findByEstabelecimentoIdAndStatusOrderByHoraEntradaAsc(estabelecimentoId, StatusFila.CHAMADO);

        List<FilaEsperaResponseDTO> resultado = new ArrayList<>(montarAguardandoComPosicao(estabelecimento, aguardando));
        for (FilaEspera chamado : chamados) {
            resultado.add(toResponseDTO(chamado, 0, 0));
        }

        return resultado;
    }

    private List<FilaEsperaResponseDTO> montarAguardandoComPosicao(Estabelecimento estabelecimento, List<FilaEspera> aguardando) {
        List<Integer> capacidadesLivres = buscarCapacidadesLivres(estabelecimento.getId());
        List<FilaEsperaResponseDTO> resultado = new ArrayList<>();

        for (int i = 0; i < aguardando.size(); i++) {
            int posicao = i + 1;
            FilaEspera entrada = aguardando.get(i);

            Integer tempoEstimado = temMesaLivreCompativel(capacidadesLivres, entrada.getQuantidadePessoas())
                    ? null
                    : posicao * estabelecimento.getTempoMedioAtendimentoMinutos();

            resultado.add(toResponseDTO(entrada, posicao, tempoEstimado));
        }

        return resultado;
    }

    private List<Integer> buscarCapacidadesLivres(String estabelecimentoId) {
        return new ArrayList<>(
                mesaRepository.findByEstabelecimentoIdAndStatusOrderByCapacidadeAsc(estabelecimentoId, StatusMesa.LIVRE)
                        .stream()
                        .map(Mesa::getCapacidade)
                        .toList());
    }

    private boolean temMesaLivreCompativel(List<Integer> capacidadesLivresSimuladas, int quantidadePessoas) {
        for (int i = 0; i < capacidadesLivresSimuladas.size(); i++) {
            if (capacidadesLivresSimuladas.get(i) >= quantidadePessoas) {
                capacidadesLivresSimuladas.remove(i);
                return true;
            }
        }
        return false;
    }

    private FilaEsperaResponseDTO toResponseDTO(FilaEspera entrada) {
        return toResponseDTO(entrada, null, null);
    }

    private FilaEsperaResponseDTO toResponseDTO(FilaEspera entrada, Integer posicao, Integer tempoEstimadoMinutos) {
        return FilaEsperaResponseDTO.builder()
                .id(entrada.getId())
                .estabelecimentoId(entrada.getEstabelecimento().getId())
                .clienteId(entrada.getCliente().getId())
                .clienteNome(entrada.getCliente().getNome())
                .mesaId(entrada.getMesa() != null ? entrada.getMesa().getId() : null)
                .quantidadePessoas(entrada.getQuantidadePessoas())
                .status(entrada.getStatus())
                .posicao(posicao)
                .tempoEsperaEstimadoMinutos(tempoEstimadoMinutos)
                .horaEntrada(entrada.getHoraEntrada())
                .horaChamado(entrada.getHoraChamado())
                .horaAtendimento(entrada.getHoraAtendimento())
                .build();
    }
}
