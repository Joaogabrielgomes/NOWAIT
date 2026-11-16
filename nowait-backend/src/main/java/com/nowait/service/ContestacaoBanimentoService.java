package com.nowait.service;

import com.nowait.dto.ContestacaoBanimentoInfoDTO;
import com.nowait.dto.ContestacaoBanimentoRequestDTO;
import com.nowait.dto.ContestacaoBanimentoResponseDTO;
import com.nowait.dto.ContestacaoResponseDTO;
import com.nowait.exception.InvalidInputException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.ContestacaoBanimento;
import com.nowait.model.StatusContestacao;
import com.nowait.model.TipoContestacao;
import com.nowait.model.Usuario;
import com.nowait.repository.ContestacaoBanimentoRepository;
import com.nowait.repository.UsuarioRepository;
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
public class ContestacaoBanimentoService {

    private static final String ENTIDADE_CONTESTACAO = "ContestacaoBanimento";

    private final ContestacaoBanimentoRepository contestacaoBanimentoRepository;
    private final UsuarioRepository              usuarioRepository;
    private final AuditoriaService               auditoriaService;

    public ContestacaoBanimentoInfoDTO buscarInfoPorToken(String token) {
        Usuario usuario = buscarUsuarioBanidoPorToken(token);
        return ContestacaoBanimentoInfoDTO.builder()
                .usuarioNome(usuario.getNome())
                .motivoBanimento(usuario.getMotivoBanimento())
                .build();
    }

    public void registrar(ContestacaoBanimentoRequestDTO dto) {
        Usuario usuario = buscarUsuarioBanidoPorToken(dto.getToken());

        ContestacaoBanimento contestacao = contestacaoBanimentoRepository.save(ContestacaoBanimento.builder()
                .usuario(usuario)
                .mensagem(dto.getMensagem())
                .build());

        auditoriaService.registrar(usuario, AcaoAuditoria.CONTESTACAO_BANIMENTO_CRIADA,
                ENTIDADE_CONTESTACAO, contestacao.getId(), null);
        log.info("Contestação de banimento registrada: usuarioId={}", usuario.getId());
    }

    public Page<ContestacaoBanimentoResponseDTO> listarPaginado(String status, String nome, int pagina, int tamanho) {
        PageRequest paginacao = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "createdAt"));
        String statusNormalizado = (status == null || status.isBlank()) ? null : status;
        String nomeNormalizado = (nome == null || nome.isBlank()) ? null : nome;
        Page<ContestacaoBanimento> resultado = contestacaoBanimentoRepository.buscar(statusNormalizado, nomeNormalizado, paginacao);
        return resultado.map(this::toResponseDTO);
    }

    public ContestacaoBanimentoResponseDTO marcarAnalisada(String id, Usuario admin) {
        ContestacaoBanimento contestacao = buscarPorId(id);
        contestacao.setStatus(StatusContestacao.ANALISADA);
        contestacao = contestacaoBanimentoRepository.save(contestacao);

        auditoriaService.registrar(admin, AcaoAuditoria.CONTESTACAO_BANIMENTO_ANALISADA,
                ENTIDADE_CONTESTACAO, id, null);
        return toResponseDTO(contestacao);
    }

    public List<ContestacaoResponseDTO> listarTodos(String status, String nome) {
        String statusNormalizado = (status == null || status.isBlank()) ? null : status;
        String nomeNormalizado = (nome == null || nome.isBlank()) ? null : nome;
        return contestacaoBanimentoRepository
                .buscarTodos(statusNormalizado, nomeNormalizado, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toUnifiedDTO)
                .toList();
    }

    private ContestacaoResponseDTO toUnifiedDTO(ContestacaoBanimento contestacao) {
        Usuario usuario = contestacao.getUsuario();
        return ContestacaoResponseDTO.builder()
                .id(contestacao.getId())
                .tipo(TipoContestacao.BANIMENTO)
                .alvoId(usuario.getId())
                .nomeAlvo(usuario.getNome())
                .emailAlvo(usuario.getEmail())
                .motivo(usuario.getMotivoBanimento())
                .aindaAtivo(usuario.isBanido())
                .mensagem(contestacao.getMensagem())
                .status(contestacao.getStatus())
                .createdAt(contestacao.getCreatedAt())
                .build();
    }

    private ContestacaoBanimento buscarPorId(String id) {
        return contestacaoBanimentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contestação de banimento", "id", id));
    }

    private Usuario buscarUsuarioBanidoPorToken(String token) {
        return usuarioRepository.findByTokenContestacaoAndBanidoTrue(token)
                .orElseThrow(() -> new InvalidInputException("Link de contestação inválido ou expirado"));
    }

    private ContestacaoBanimentoResponseDTO toResponseDTO(ContestacaoBanimento contestacao) {
        Usuario usuario = contestacao.getUsuario();
        return ContestacaoBanimentoResponseDTO.builder()
                .id(contestacao.getId())
                .usuarioId(usuario.getId())
                .usuarioNome(usuario.getNome())
                .usuarioEmail(usuario.getEmail())
                .motivoBanimento(usuario.getMotivoBanimento())
                .usuarioAindaBanido(usuario.isBanido())
                .mensagem(contestacao.getMensagem())
                .status(contestacao.getStatus())
                .createdAt(contestacao.getCreatedAt())
                .build();
    }
}
