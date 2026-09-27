package com.nowait.service;

import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.dto.LogAuditoriaResponseDTO;
import com.nowait.dto.UsuarioResponseDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.LogAuditoria;
import com.nowait.model.Usuario;
import com.nowait.repository.AgendamentoRepository;
import com.nowait.repository.EstabelecimentoRepository;
import com.nowait.repository.FilaEsperaRepository;
import com.nowait.repository.LogAuditoriaRepository;
import com.nowait.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminService {

    private static final String ENTIDADE_USUARIO = "Usuario";

    private final UsuarioRepository         usuarioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final FilaEsperaRepository      filaEsperaRepository;
    private final AgendamentoRepository     agendamentoRepository;
    private final LogAuditoriaRepository    logAuditoriaRepository;
    private final UsuarioService            usuarioService;
    private final EstabelecimentoService    estabelecimentoService;
    private final AuditoriaService          auditoriaService;

    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(usuarioService::toResponseDTO)
                .toList();
    }

    public void removerUsuario(String id, Usuario admin) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", id));

        validarSemHistoricoVinculado(usuario);

        usuarioRepository.delete(usuario);
        auditoriaService.registrar(admin, AcaoAuditoria.ADMIN_USUARIO_REMOVIDO, ENTIDADE_USUARIO, id, null);
    }

    private void validarSemHistoricoVinculado(Usuario usuario) {
        if (estabelecimentoRepository.existsByUsuarioId(usuario.getId())) {
            throw new BusinessRuleException(
                    "Não é possível remover: este usuário possui um estabelecimento cadastrado");
        }
        if (filaEsperaRepository.existsByClienteId(usuario.getId())) {
            throw new BusinessRuleException(
                    "Não é possível remover: este usuário possui histórico de fila de espera");
        }
        if (agendamentoRepository.existsByClienteId(usuario.getId())) {
            throw new BusinessRuleException(
                    "Não é possível remover: este usuário possui histórico de agendamentos");
        }
    }

    public List<EstabelecimentoResponseDTO> listarEstabelecimentos() {
        return estabelecimentoService.listarTodos();
    }

    public Page<LogAuditoriaResponseDTO> listarAuditoria(String usuarioId, AcaoAuditoria acao, int pagina, int tamanho) {
        Page<LogAuditoria> resultado = logAuditoriaRepository.buscarComFiltros(
                usuarioId, acao, PageRequest.of(pagina, tamanho));
        return resultado.map(this::toLogResponseDTO);
    }

    private LogAuditoriaResponseDTO toLogResponseDTO(LogAuditoria log) {
        return LogAuditoriaResponseDTO.builder()
                .id(log.getId())
                .usuarioId(log.getUsuarioId())
                .acao(log.getAcao().name())
                .entidadeTipo(log.getEntidadeTipo())
                .entidadeId(log.getEntidadeId())
                .detalhes(log.getDetalhes())
                .ip(log.getIp())
                .criadoEm(log.getCriadoEm())
                .build();
    }
}
