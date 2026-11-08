package com.nowait.service;

import com.nowait.dto.AvaliacaoPendenteDTO;
import com.nowait.dto.AvaliacaoRequestDTO;
import com.nowait.dto.AvaliacaoResponseDTO;
import com.nowait.dto.AvaliacaoResumoDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.DuplicateResourceException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.exception.UnauthorizedOperationException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.Agendamento;
import com.nowait.model.Avaliacao;
import com.nowait.model.FilaEspera;
import com.nowait.model.StatusAgendamento;
import com.nowait.model.StatusFila;
import com.nowait.model.Usuario;
import com.nowait.repository.AgendamentoRepository;
import com.nowait.repository.AvaliacaoRepository;
import com.nowait.repository.FilaEsperaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AvaliacaoService {

    private static final String ENTIDADE_AVALIACAO = "Avaliacao";
    private static final String TIPO_FILA = "FILA";
    private static final String TIPO_AGENDAMENTO = "AGENDAMENTO";
    private static final String MENSAGEM_NAO_LIBERADA =
            "Só é possível avaliar depois que o estabelecimento liberar sua mesa";
    private static final String MENSAGEM_SEM_PERMISSAO = "Você não pode avaliar esta visita";

    private final AvaliacaoRepository   avaliacaoRepository;
    private final FilaEsperaRepository  filaEsperaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AuditoriaService      auditoriaService;

    public AvaliacaoResponseDTO avaliar(AvaliacaoRequestDTO dto, Usuario cliente) {
        Avaliacao avaliacao = dto.getFilaId() != null
                ? construirAPartirDaFila(dto, cliente)
                : construirAPartirDoAgendamento(dto, cliente);

        avaliacao = avaliacaoRepository.save(avaliacao);
        log.info("Avaliação criada: id={} cliente={} estabelecimento={}",
                avaliacao.getId(), cliente.getId(), avaliacao.getEstabelecimento().getId());

        auditoriaService.registrar(cliente, AcaoAuditoria.AVALIACAO_CRIADA, ENTIDADE_AVALIACAO,
                avaliacao.getId(), "nota=" + avaliacao.getNota());
        return toResponseDTO(avaliacao);
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoPendenteDTO> listarPendentes(Usuario cliente) {
        List<AvaliacaoPendenteDTO> pendentes = new ArrayList<>();
        pendentes.addAll(listarPendentesDeFila(cliente));
        pendentes.addAll(listarPendentesDeAgendamento(cliente));
        return pendentes;
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponseDTO> listarPorEstabelecimento(String estabelecimentoId) {
        return avaliacaoRepository.findByEstabelecimentoIdOrderByCreatedAtDesc(estabelecimentoId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AvaliacaoResumoDTO resumoPublico(String estabelecimentoId) {
        return AvaliacaoResumoDTO.builder()
                .mediaNota(avaliacaoRepository.calcularMediaPorEstabelecimento(estabelecimentoId))
                .totalAvaliacoes(avaliacaoRepository.countByEstabelecimentoId(estabelecimentoId))
                .build();
    }

    private Avaliacao construirAPartirDaFila(AvaliacaoRequestDTO dto, Usuario cliente) {
        FilaEspera entrada = filaEsperaRepository.findById(dto.getFilaId())
                .orElseThrow(() -> new ResourceNotFoundException("Entrada na fila", "id", dto.getFilaId()));

        validarPermissaoCliente(entrada.getCliente().getId(), cliente.getId());
        validarLiberada(entrada.getAvaliacaoLiberada());
        if (avaliacaoRepository.existsByFilaId(entrada.getId())) {
            throw new DuplicateResourceException("Avaliação", "fila", entrada.getId());
        }

        return Avaliacao.builder()
                .estabelecimento(entrada.getEstabelecimento())
                .cliente(cliente)
                .fila(entrada)
                .nota(dto.getNota())
                .comentario(dto.getComentario())
                .build();
    }

    private Avaliacao construirAPartirDoAgendamento(AvaliacaoRequestDTO dto, Usuario cliente) {
        Agendamento agendamento = agendamentoRepository.findById(dto.getAgendamentoId())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento", "id", dto.getAgendamentoId()));

        validarPermissaoCliente(agendamento.getCliente().getId(), cliente.getId());
        validarLiberada(agendamento.getAvaliacaoLiberada());
        if (avaliacaoRepository.existsByAgendamentoId(agendamento.getId())) {
            throw new DuplicateResourceException("Avaliação", "agendamento", agendamento.getId());
        }

        return Avaliacao.builder()
                .estabelecimento(agendamento.getEstabelecimento())
                .cliente(cliente)
                .agendamento(agendamento)
                .nota(dto.getNota())
                .comentario(dto.getComentario())
                .build();
    }

    private void validarPermissaoCliente(String clienteIdDoRegistro, String clienteIdSolicitante) {
        if (!clienteIdDoRegistro.equals(clienteIdSolicitante)) {
            throw new UnauthorizedOperationException(MENSAGEM_SEM_PERMISSAO);
        }
    }

    private void validarLiberada(Boolean avaliacaoLiberada) {
        if (!Boolean.TRUE.equals(avaliacaoLiberada)) {
            throw new BusinessRuleException(MENSAGEM_NAO_LIBERADA);
        }
    }

    private List<AvaliacaoPendenteDTO> listarPendentesDeFila(Usuario cliente) {
        return filaEsperaRepository
                .findByClienteIdAndStatusAndAvaliacaoLiberadaTrueOrderByHoraAtendimentoDesc(
                        cliente.getId(), StatusFila.ATENDIDO)
                .stream()
                .filter(entrada -> !avaliacaoRepository.existsByFilaId(entrada.getId()))
                .map(entrada -> AvaliacaoPendenteDTO.builder()
                        .tipo(TIPO_FILA)
                        .referenciaId(entrada.getId())
                        .estabelecimentoId(entrada.getEstabelecimento().getId())
                        .estabelecimentoNome(entrada.getEstabelecimento().getNome())
                        .data(entrada.getHoraAtendimento())
                        .build())
                .toList();
    }

    private List<AvaliacaoPendenteDTO> listarPendentesDeAgendamento(Usuario cliente) {
        return agendamentoRepository
                .findByClienteIdAndStatusAndAvaliacaoLiberadaTrueOrderByUpdatedAtDesc(
                        cliente.getId(), StatusAgendamento.CONCLUIDO)
                .stream()
                .filter(agendamento -> !avaliacaoRepository.existsByAgendamentoId(agendamento.getId()))
                .map(agendamento -> AvaliacaoPendenteDTO.builder()
                        .tipo(TIPO_AGENDAMENTO)
                        .referenciaId(agendamento.getId())
                        .estabelecimentoId(agendamento.getEstabelecimento().getId())
                        .estabelecimentoNome(agendamento.getEstabelecimento().getNome())
                        .data(agendamento.getDataHora())
                        .build())
                .toList();
    }

    private AvaliacaoResponseDTO toResponseDTO(Avaliacao avaliacao) {
        return AvaliacaoResponseDTO.builder()
                .id(avaliacao.getId())
                .estabelecimentoId(avaliacao.getEstabelecimento().getId())
                .estabelecimentoNome(avaliacao.getEstabelecimento().getNome())
                .clienteId(avaliacao.getCliente().getId())
                .clienteNome(avaliacao.getCliente().getNome())
                .nota(avaliacao.getNota())
                .comentario(avaliacao.getComentario())
                .createdAt(avaliacao.getCreatedAt())
                .build();
    }
}
