package com.nowait.service;

import com.nowait.dto.AcaoPorDiaDTO;
import com.nowait.dto.CrescimentoDiaDTO;
import com.nowait.dto.DashboardResumoDTO;
import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.dto.LogAuditoriaResponseDTO;
import com.nowait.dto.UsuarioResponseDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.LogAuditoria;
import com.nowait.model.Perfil;
import com.nowait.model.StatusFila;
import com.nowait.model.Usuario;
import com.nowait.repository.AgendamentoRepository;
import com.nowait.repository.ContagemPorDiaProjection;
import com.nowait.repository.EstabelecimentoRepository;
import com.nowait.repository.FilaEsperaRepository;
import com.nowait.repository.LogAuditoriaRepository;
import com.nowait.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminService {

    private static final String ENTIDADE_USUARIO = "Usuario";
    private static final int    DIAS_HISTORICO_GRAFICO = 14;
    private static final List<String> STATUS_FILA_ATIVOS = List.of(StatusFila.AGUARDANDO, StatusFila.CHAMADO);
    private static final DateTimeFormatter FORMATO_DATA_HORA_CSV = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String CABECALHO_CSV = "Data;Usuario;Acao;Entidade;EntidadeId;Detalhes;IP";

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

    public byte[] exportarAuditoriaCsv(String usuarioId, AcaoAuditoria acao) {
        List<LogAuditoria> logs = logAuditoriaRepository.buscarTodosComFiltros(usuarioId, acao);

        StringBuilder csv = new StringBuilder("﻿").append(CABECALHO_CSV).append('\n');
        for (LogAuditoria log : logs) {
            csv.append(montarLinhaCsv(log)).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String montarLinhaCsv(LogAuditoria log) {
        return String.join(";",
                escaparCampoCsv(log.getCriadoEm().format(FORMATO_DATA_HORA_CSV)),
                escaparCampoCsv(log.getUsuarioId()),
                escaparCampoCsv(log.getAcao().name()),
                escaparCampoCsv(log.getEntidadeTipo()),
                escaparCampoCsv(log.getEntidadeId()),
                escaparCampoCsv(log.getDetalhes()),
                escaparCampoCsv(log.getIp()));
    }

    private String escaparCampoCsv(String valor) {
        if (valor == null) {
            return "";
        }
        String escapado = valor.replace("\"", "\"\"");
        boolean precisaAspas = escapado.contains(";") || escapado.contains("\"") || escapado.contains("\n");
        return precisaAspas ? "\"" + escapado + "\"" : escapado;
    }

    public DashboardResumoDTO obterResumoDashboard() {
        Map<String, Long> usuariosPorPerfil = new LinkedHashMap<>();
        usuariosPorPerfil.put(Perfil.CLIENTE, usuarioRepository.countByRole(Perfil.CLIENTE));
        usuariosPorPerfil.put(Perfil.ESTABELECIMENTO, usuarioRepository.countByRole(Perfil.ESTABELECIMENTO));
        usuariosPorPerfil.put(Perfil.ADMIN, usuarioRepository.countByRole(Perfil.ADMIN));

        long pessoasNaFilaAgora = filaEsperaRepository.somarPessoasPorStatus(STATUS_FILA_ATIVOS);

        List<LogAuditoriaResponseDTO> atividadeRecente = logAuditoriaRepository.findTop20ByOrderByCriadoEmDesc()
                .stream()
                .map(this::toLogResponseDTO)
                .toList();

        LocalDateTime desde = LocalDateTime.now().minusDays(DIAS_HISTORICO_GRAFICO);
        List<AcaoPorDiaDTO> acoesPorDia = logAuditoriaRepository.contarAcoesPorDia(desde).stream()
                .map(p -> AcaoPorDiaDTO.builder()
                        .dia(p.getDia())
                        .acao(p.getAcao())
                        .quantidade(p.getQuantidade())
                        .build())
                .toList();

        return DashboardResumoDTO.builder()
                .usuariosPorPerfil(usuariosPorPerfil)
                .estabelecimentosAbertos(estabelecimentoRepository.countByAberto(true))
                .estabelecimentosFechados(estabelecimentoRepository.countByAberto(false))
                .pessoasNaFilaAgora(pessoasNaFilaAgora)
                .atividadeRecente(atividadeRecente)
                .acoesPorDia(acoesPorDia)
                .crescimentoPorDia(obterCrescimentoPorDia(desde))
                .build();
    }

    private List<CrescimentoDiaDTO> obterCrescimentoPorDia(LocalDateTime desde) {
        Map<LocalDate, Long> usuariosPorDia = agruparContagemPorDia(usuarioRepository.contarNovosPorDia(desde));
        Map<LocalDate, Long> estabelecimentosPorDia = agruparContagemPorDia(estabelecimentoRepository.contarNovosPorDia(desde));

        TreeSet<LocalDate> dias = new TreeSet<>();
        dias.addAll(usuariosPorDia.keySet());
        dias.addAll(estabelecimentosPorDia.keySet());

        return dias.stream()
                .map(dia -> CrescimentoDiaDTO.builder()
                        .dia(dia)
                        .novosUsuarios(usuariosPorDia.getOrDefault(dia, 0L))
                        .novosEstabelecimentos(estabelecimentosPorDia.getOrDefault(dia, 0L))
                        .build())
                .toList();
    }

    private Map<LocalDate, Long> agruparContagemPorDia(List<ContagemPorDiaProjection> projecoes) {
        return projecoes.stream()
                .collect(Collectors.toMap(ContagemPorDiaProjection::getDia, ContagemPorDiaProjection::getQuantidade));
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
