package com.nowait.service;

import com.nowait.dto.AcaoPorDiaDTO;
import com.nowait.dto.ContestacaoBanimentoResponseDTO;
import com.nowait.dto.ContestacaoRejeicaoEstabelecimentoResponseDTO;
import com.nowait.dto.ContestacaoResponseDTO;
import com.nowait.dto.CrescimentoDiaDTO;
import com.nowait.dto.DashboardResumoDTO;
import com.nowait.dto.DenunciaResolucaoDTO;
import com.nowait.dto.DenunciaResponseDTO;
import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.dto.LogAuditoriaResponseDTO;
import com.nowait.dto.UsuarioResponseDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.LogAuditoria;
import com.nowait.model.Perfil;
import com.nowait.model.StatusDenuncia;
import com.nowait.model.StatusFila;
import com.nowait.model.Usuario;
import com.nowait.repository.AgendamentoRepository;
import com.nowait.repository.ContagemPorDiaProjection;
import com.nowait.repository.EstabelecimentoRepository;
import com.nowait.repository.FilaEsperaRepository;
import com.nowait.repository.LogAuditoriaRepository;
import com.nowait.repository.UsuarioRepository;
import com.nowait.repository.UsuarioSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminService {

    private static final String ENTIDADE_USUARIO = "Usuario";
    private static final String ENTIDADE_ESTABELECIMENTO = "Estabelecimento";
    private static final String ENTIDADE_DENUNCIA = "Denuncia";
    private static final int    DIAS_HISTORICO_GRAFICO = 14;
    private static final List<String> STATUS_FILA_ATIVOS = List.of(StatusFila.AGUARDANDO, StatusFila.CHAMADO);
    private static final DateTimeFormatter FORMATO_DATA_HORA_CSV = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String CABECALHO_CSV = "Data;UsuarioId;UsuarioNome;Acao;Entidade;EntidadeId;Detalhes;IP";
    private static final String USUARIO_REMOVIDO = "Usuário removido";

    private final UsuarioRepository         usuarioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final FilaEsperaRepository      filaEsperaRepository;
    private final AgendamentoRepository     agendamentoRepository;
    private final LogAuditoriaRepository    logAuditoriaRepository;
    private final UsuarioService            usuarioService;
    private final EstabelecimentoService    estabelecimentoService;
    private final DenunciaService           denunciaService;
    private final AuditoriaService          auditoriaService;
    private final ContestacaoBanimentoService contestacaoBanimentoService;
    private final ContestacaoRejeicaoEstabelecimentoService contestacaoRejeicaoEstabelecimentoService;

    private static final String CAMPO_DATA_CRIACAO = "createdAt";

    private String resolverCampoOrdenacao(String campo) {
        return CAMPO_DATA_CRIACAO.equalsIgnoreCase(campo) ? CAMPO_DATA_CRIACAO : "nome";
    }

    public Page<UsuarioResponseDTO> listarUsuarios(String role, String nome, String campo, String ordem, int pagina, int tamanho) {
        Sort.Direction direcao = "desc".equalsIgnoreCase(ordem) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest paginacao = PageRequest.of(pagina, tamanho, Sort.by(direcao, resolverCampoOrdenacao(campo)));
        Specification<Usuario> filtro = Specification
                .where(UsuarioSpecifications.comRole(role))
                .and(UsuarioSpecifications.comNome(nome));
        return usuarioRepository.findAll(filtro, paginacao).map(usuarioService::toResponseDTO);
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

    public UsuarioResponseDTO banirUsuario(String id, String motivo, Usuario admin) {
        UsuarioResponseDTO resultado = usuarioService.banir(id, motivo);
        auditoriaService.registrar(admin, AcaoAuditoria.ADMIN_USUARIO_BANIDO, ENTIDADE_USUARIO, id, motivo);
        return resultado;
    }

    public UsuarioResponseDTO desbanirUsuario(String id, Usuario admin) {
        UsuarioResponseDTO resultado = usuarioService.desbanir(id);
        auditoriaService.registrar(admin, AcaoAuditoria.ADMIN_USUARIO_DESBANIDO, ENTIDADE_USUARIO, id, null);
        return resultado;
    }

    public Page<DenunciaResponseDTO> listarDenuncias(String status, String nome, int pagina, int tamanho) {
        return denunciaService.listarPaginado(status, nome, pagina, tamanho);
    }

    public Page<ContestacaoBanimentoResponseDTO> listarContestacoesBanimento(String status, String nome, int pagina, int tamanho) {
        return contestacaoBanimentoService.listarPaginado(status, nome, pagina, tamanho);
    }

    public ContestacaoBanimentoResponseDTO marcarContestacaoAnalisada(String id, Usuario admin) {
        return contestacaoBanimentoService.marcarAnalisada(id, admin);
    }

    public ContestacaoRejeicaoEstabelecimentoResponseDTO marcarContestacaoRejeicaoEstabelecimentoAnalisada(
            String id, Usuario admin) {
        return contestacaoRejeicaoEstabelecimentoService.marcarAnalisada(id, admin);
    }

    public Page<ContestacaoResponseDTO> listarContestacoes(String status, String nome, int pagina, int tamanho) {
        List<ContestacaoResponseDTO> todas = new ArrayList<>();
        todas.addAll(contestacaoBanimentoService.listarTodos(status, nome));
        todas.addAll(contestacaoRejeicaoEstabelecimentoService.listarTodos(status, nome));
        todas.sort(Comparator.comparing(ContestacaoResponseDTO::getCreatedAt).reversed());

        int inicio = Math.min(pagina * tamanho, todas.size());
        int fim = Math.min(inicio + tamanho, todas.size());
        List<ContestacaoResponseDTO> itensPagina = todas.subList(inicio, fim);

        return new PageImpl<>(itensPagina, PageRequest.of(pagina, tamanho), todas.size());
    }

    public DenunciaResponseDTO resolverDenuncia(String id, Usuario admin, DenunciaResolucaoDTO dto) {
        DenunciaResponseDTO resolvida = denunciaService.resolver(id, admin, dto);
        auditoriaService.registrar(admin, AcaoAuditoria.DENUNCIA_RESOLVIDA, ENTIDADE_DENUNCIA, id,
                "status=" + resolvida.getStatus());

        boolean deveBanir = dto.isBanirUsuarioAlvo()
                && StatusDenuncia.APROVADA.equals(resolvida.getStatus())
                && resolvida.getUsuarioAlvoId() != null;
        if (deveBanir) {
            String motivoBanimento = (dto.getObservacaoAdmin() != null && !dto.getObservacaoAdmin().isBlank())
                    ? dto.getObservacaoAdmin()
                    : "Denúncia aprovada: " + resolvida.getMotivo();
            usuarioService.banir(resolvida.getUsuarioAlvoId(), motivoBanimento);
            auditoriaService.registrar(admin, AcaoAuditoria.ADMIN_USUARIO_BANIDO, ENTIDADE_USUARIO,
                    resolvida.getUsuarioAlvoId(), "decorrente da denúncia " + id);
        }
        return resolvida;
    }

    public Page<EstabelecimentoResponseDTO> listarEstabelecimentos(
            String statusAprovacao, String nome, String campo, String ordem, int pagina, int tamanho) {
        Sort.Direction direcao = "desc".equalsIgnoreCase(ordem) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return estabelecimentoService.listarPaginado(
                statusAprovacao, nome, resolverCampoOrdenacao(campo), direcao, pagina, tamanho);
    }

    public EstabelecimentoResponseDTO aprovarEstabelecimento(String id, Usuario admin) {
        EstabelecimentoResponseDTO aprovado = estabelecimentoService.aprovar(id);
        auditoriaService.registrar(admin, AcaoAuditoria.ADMIN_ESTABELECIMENTO_APROVADO, ENTIDADE_ESTABELECIMENTO, id, null);
        return aprovado;
    }

    public EstabelecimentoResponseDTO rejeitarEstabelecimento(String id, String motivo, Usuario admin) {
        EstabelecimentoResponseDTO rejeitado = estabelecimentoService.rejeitar(id, motivo);
        auditoriaService.registrar(admin, AcaoAuditoria.ADMIN_ESTABELECIMENTO_REJEITADO, ENTIDADE_ESTABELECIMENTO, id, motivo);
        return rejeitado;
    }

    public Page<LogAuditoriaResponseDTO> listarAuditoria(String usuarioId, AcaoAuditoria acao, int pagina, int tamanho) {
        Page<LogAuditoria> resultado = logAuditoriaRepository.buscarComFiltros(
                usuarioId, acao, PageRequest.of(pagina, tamanho));
        Map<String, String> nomesPorUsuarioId = buscarNomesPorUsuarioId(resultado.getContent());
        return resultado.map(log -> toLogResponseDTO(log, nomesPorUsuarioId));
    }

    public byte[] exportarAuditoriaCsv(String usuarioId, AcaoAuditoria acao) {
        List<LogAuditoria> logs = logAuditoriaRepository.buscarTodosComFiltros(usuarioId, acao);
        Map<String, String> nomesPorUsuarioId = buscarNomesPorUsuarioId(logs);

        StringBuilder csv = new StringBuilder("﻿").append(CABECALHO_CSV).append('\n');
        for (LogAuditoria log : logs) {
            csv.append(montarLinhaCsv(log, nomesPorUsuarioId)).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, String> buscarNomesPorUsuarioId(List<LogAuditoria> logs) {
        List<String> usuarioIds = logs.stream()
                .map(LogAuditoria::getUsuarioId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return usuarioRepository.findAllById(usuarioIds).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNome));
    }

    private String resolverNomeUsuario(String usuarioId, Map<String, String> nomesPorUsuarioId) {
        if (usuarioId == null) {
            return null;
        }
        return nomesPorUsuarioId.getOrDefault(usuarioId, USUARIO_REMOVIDO);
    }

    private String montarLinhaCsv(LogAuditoria log, Map<String, String> nomesPorUsuarioId) {
        return String.join(";",
                escaparCampoCsv(log.getCriadoEm().format(FORMATO_DATA_HORA_CSV)),
                escaparCampoCsv(log.getUsuarioId()),
                escaparCampoCsv(resolverNomeUsuario(log.getUsuarioId(), nomesPorUsuarioId)),
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

        List<LogAuditoria> logsRecentes = logAuditoriaRepository.findTop20ByOrderByCriadoEmDesc();
        Map<String, String> nomesAtividadeRecente = buscarNomesPorUsuarioId(logsRecentes);
        List<LogAuditoriaResponseDTO> atividadeRecente = logsRecentes.stream()
                .map(log -> toLogResponseDTO(log, nomesAtividadeRecente))
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

    private LogAuditoriaResponseDTO toLogResponseDTO(LogAuditoria log, Map<String, String> nomesPorUsuarioId) {
        return LogAuditoriaResponseDTO.builder()
                .id(log.getId())
                .usuarioId(log.getUsuarioId())
                .usuarioNome(resolverNomeUsuario(log.getUsuarioId(), nomesPorUsuarioId))
                .acao(log.getAcao().name())
                .entidadeTipo(log.getEntidadeTipo())
                .entidadeId(log.getEntidadeId())
                .detalhes(log.getDetalhes())
                .ip(log.getIp())
                .criadoEm(log.getCriadoEm())
                .build();
    }
}
