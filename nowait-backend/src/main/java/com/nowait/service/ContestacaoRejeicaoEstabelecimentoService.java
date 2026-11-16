package com.nowait.service;

import com.nowait.dto.ContestacaoResponseDTO;
import com.nowait.dto.ContestacaoRejeicaoEstabelecimentoInfoDTO;
import com.nowait.dto.ContestacaoRejeicaoEstabelecimentoRequestDTO;
import com.nowait.dto.ContestacaoRejeicaoEstabelecimentoResponseDTO;
import com.nowait.exception.InvalidInputException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.ContestacaoRejeicaoEstabelecimento;
import com.nowait.model.Estabelecimento;
import com.nowait.model.StatusAprovacaoEstabelecimento;
import com.nowait.model.StatusContestacao;
import com.nowait.model.TipoContestacao;
import com.nowait.model.Usuario;
import com.nowait.repository.ContestacaoRejeicaoEstabelecimentoRepository;
import com.nowait.repository.EstabelecimentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ContestacaoRejeicaoEstabelecimentoService {

    private static final String ENTIDADE_CONTESTACAO = "ContestacaoRejeicaoEstabelecimento";

    private final ContestacaoRejeicaoEstabelecimentoRepository contestacaoRepository;
    private final EstabelecimentoRepository                    estabelecimentoRepository;
    private final AuditoriaService                             auditoriaService;

    public ContestacaoRejeicaoEstabelecimentoInfoDTO buscarInfoPorToken(String token) {
        Estabelecimento estabelecimento = buscarEstabelecimentoRejeitadoPorToken(token);
        return ContestacaoRejeicaoEstabelecimentoInfoDTO.builder()
                .estabelecimentoNome(estabelecimento.getNome())
                .motivoRejeicao(estabelecimento.getMotivoRejeicao())
                .build();
    }

    public void registrar(ContestacaoRejeicaoEstabelecimentoRequestDTO dto) {
        Estabelecimento estabelecimento = buscarEstabelecimentoRejeitadoPorToken(dto.getToken());

        ContestacaoRejeicaoEstabelecimento contestacao = contestacaoRepository.save(
                ContestacaoRejeicaoEstabelecimento.builder()
                        .estabelecimento(estabelecimento)
                        .mensagem(dto.getMensagem())
                        .build());

        auditoriaService.registrar(estabelecimento.getUsuario(), AcaoAuditoria.CONTESTACAO_REJEICAO_ESTABELECIMENTO_CRIADA,
                ENTIDADE_CONTESTACAO, contestacao.getId(), null);
        log.info("Contestação de rejeição de estabelecimento registrada: estabelecimentoId={}", estabelecimento.getId());
    }

    public Page<ContestacaoRejeicaoEstabelecimentoResponseDTO> listarPaginado(
            String status, String nome, int pagina, int tamanho) {
        PageRequest paginacao = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "createdAt"));
        String statusNormalizado = (status == null || status.isBlank()) ? null : status;
        String nomeNormalizado = (nome == null || nome.isBlank()) ? null : nome;
        Page<ContestacaoRejeicaoEstabelecimento> resultado =
                contestacaoRepository.buscar(statusNormalizado, nomeNormalizado, paginacao);
        return resultado.map(this::toResponseDTO);
    }

    public List<ContestacaoResponseDTO> listarTodos(String status, String nome) {
        String statusNormalizado = (status == null || status.isBlank()) ? null : status;
        String nomeNormalizado = (nome == null || nome.isBlank()) ? null : nome;
        return contestacaoRepository
                .buscarTodos(statusNormalizado, nomeNormalizado, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toUnifiedDTO)
                .toList();
    }

    public ContestacaoRejeicaoEstabelecimentoResponseDTO marcarAnalisada(String id, Usuario admin) {
        ContestacaoRejeicaoEstabelecimento contestacao = buscarPorId(id);
        contestacao.setStatus(StatusContestacao.ANALISADA);
        contestacao = contestacaoRepository.save(contestacao);

        auditoriaService.registrar(admin, AcaoAuditoria.CONTESTACAO_REJEICAO_ESTABELECIMENTO_ANALISADA,
                ENTIDADE_CONTESTACAO, id, null);
        return toResponseDTO(contestacao);
    }

    private ContestacaoRejeicaoEstabelecimento buscarPorId(String id) {
        return contestacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contestação de rejeição de estabelecimento", "id", id));
    }

    private Estabelecimento buscarEstabelecimentoRejeitadoPorToken(String token) {
        return estabelecimentoRepository
                .findByTokenContestacaoRejeicaoAndStatusAprovacao(token, StatusAprovacaoEstabelecimento.REJEITADO)
                .orElseThrow(() -> new InvalidInputException("Link de contestação inválido ou expirado"));
    }

    private ContestacaoRejeicaoEstabelecimentoResponseDTO toResponseDTO(ContestacaoRejeicaoEstabelecimento contestacao) {
        Estabelecimento estabelecimento = contestacao.getEstabelecimento();
        return ContestacaoRejeicaoEstabelecimentoResponseDTO.builder()
                .id(contestacao.getId())
                .estabelecimentoId(estabelecimento.getId())
                .estabelecimentoNome(estabelecimento.getNome())
                .estabelecimentoEmail(estabelecimento.getUsuario().getEmail())
                .motivoRejeicao(estabelecimento.getMotivoRejeicao())
                .estabelecimentoAindaRejeitado(
                        StatusAprovacaoEstabelecimento.REJEITADO.equals(estabelecimento.getStatusAprovacao()))
                .mensagem(contestacao.getMensagem())
                .status(contestacao.getStatus())
                .createdAt(contestacao.getCreatedAt())
                .build();
    }

    private ContestacaoResponseDTO toUnifiedDTO(ContestacaoRejeicaoEstabelecimento contestacao) {
        Estabelecimento estabelecimento = contestacao.getEstabelecimento();
        return ContestacaoResponseDTO.builder()
                .id(contestacao.getId())
                .tipo(TipoContestacao.REJEICAO_ESTABELECIMENTO)
                .alvoId(estabelecimento.getId())
                .nomeAlvo(estabelecimento.getNome())
                .emailAlvo(estabelecimento.getUsuario().getEmail())
                .motivo(estabelecimento.getMotivoRejeicao())
                .aindaAtivo(StatusAprovacaoEstabelecimento.REJEITADO.equals(estabelecimento.getStatusAprovacao()))
                .mensagem(contestacao.getMensagem())
                .status(contestacao.getStatus())
                .createdAt(contestacao.getCreatedAt())
                .build();
    }
}
