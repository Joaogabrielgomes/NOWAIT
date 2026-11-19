package com.nowait.service;

import com.nowait.dto.DenunciaResolucaoDTO;
import com.nowait.dto.DenunciaResponseDTO;
import com.nowait.dto.UsuarioResponseDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.LogAuditoria;
import com.nowait.model.StatusDenuncia;
import com.nowait.model.Usuario;
import com.nowait.repository.AgendamentoRepository;
import com.nowait.repository.EstabelecimentoRepository;
import com.nowait.repository.FilaEsperaRepository;
import com.nowait.repository.LogAuditoriaRepository;
import com.nowait.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes do AdminService: remoção de usuário (bloqueada quando há histórico vinculado),
 * banimento/desbanimento (delegação + auditoria), resolução de denúncia com banimento em
 * cascata, e exportação de log de auditoria para CSV (incluindo o escape de campos com ";").
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstabelecimentoRepository estabelecimentoRepository;
    @Mock private FilaEsperaRepository filaEsperaRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private LogAuditoriaRepository logAuditoriaRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private EstabelecimentoService estabelecimentoService;
    @Mock private DenunciaService denunciaService;
    @Mock private AuditoriaService auditoriaService;
    @Mock private ContestacaoBanimentoService contestacaoBanimentoService;
    @Mock private ContestacaoRejeicaoEstabelecimentoService contestacaoRejeicaoEstabelecimentoService;

    @InjectMocks
    private AdminService adminService;

    private Usuario admin;

    @BeforeEach
    void setUp() {
        admin = Usuario.builder().id("admin-1").nome("Admin").build();
    }

    @Test
    void removerUsuario_lancaExcecaoQuandoPossuiEstabelecimento() {
        Usuario alvo = Usuario.builder().id("usr-1").build();
        when(usuarioRepository.findById("usr-1")).thenReturn(Optional.of(alvo));
        when(estabelecimentoRepository.existsByUsuarioId("usr-1")).thenReturn(true);

        assertThatThrownBy(() -> adminService.removerUsuario("usr-1", admin))
                .isInstanceOf(BusinessRuleException.class);

        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    @Test
    void removerUsuario_lancaExcecaoQuandoPossuiHistoricoDeAgendamento() {
        Usuario alvo = Usuario.builder().id("usr-1").build();
        when(usuarioRepository.findById("usr-1")).thenReturn(Optional.of(alvo));
        when(estabelecimentoRepository.existsByUsuarioId("usr-1")).thenReturn(false);
        when(filaEsperaRepository.existsByClienteId("usr-1")).thenReturn(false);
        when(agendamentoRepository.existsByClienteId("usr-1")).thenReturn(true);

        assertThatThrownBy(() -> adminService.removerUsuario("usr-1", admin))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void removerUsuario_removeERegistraAuditoriaQuandoSemHistorico() {
        Usuario alvo = Usuario.builder().id("usr-1").build();
        when(usuarioRepository.findById("usr-1")).thenReturn(Optional.of(alvo));
        when(estabelecimentoRepository.existsByUsuarioId("usr-1")).thenReturn(false);
        when(filaEsperaRepository.existsByClienteId("usr-1")).thenReturn(false);
        when(agendamentoRepository.existsByClienteId("usr-1")).thenReturn(false);

        adminService.removerUsuario("usr-1", admin);

        verify(usuarioRepository).delete(alvo);
        verify(auditoriaService).registrar(admin, AcaoAuditoria.ADMIN_USUARIO_REMOVIDO, "Usuario", "usr-1", null);
    }

    @Test
    void banirUsuario_delegaParaUsuarioServiceERegistraAuditoria() {
        UsuarioResponseDTO resposta = UsuarioResponseDTO.builder().id("usr-1").banido(true).build();
        when(usuarioService.banir("usr-1", "motivo")).thenReturn(resposta);

        UsuarioResponseDTO resultado = adminService.banirUsuario("usr-1", "motivo", admin);

        assertThat(resultado.isBanido()).isTrue();
        verify(auditoriaService).registrar(admin, AcaoAuditoria.ADMIN_USUARIO_BANIDO, "Usuario", "usr-1", "motivo");
    }

    @Test
    void desbanirUsuario_delegaParaUsuarioServiceERegistraAuditoria() {
        UsuarioResponseDTO resposta = UsuarioResponseDTO.builder().id("usr-1").banido(false).build();
        when(usuarioService.desbanir("usr-1")).thenReturn(resposta);

        UsuarioResponseDTO resultado = adminService.desbanirUsuario("usr-1", admin);

        assertThat(resultado.isBanido()).isFalse();
        verify(auditoriaService).registrar(admin, AcaoAuditoria.ADMIN_USUARIO_DESBANIDO, "Usuario", "usr-1", null);
    }

    @Test
    void resolverDenuncia_naoBaneQuandoBanirUsuarioAlvoNaoFoiMarcado() {
        DenunciaResolucaoDTO dto = new DenunciaResolucaoDTO();
        dto.setAprovar(true);
        dto.setBanirUsuarioAlvo(false);
        DenunciaResponseDTO resolvida = DenunciaResponseDTO.builder()
                .id("d-1").status(StatusDenuncia.APROVADA).usuarioAlvoId("usr-alvo").motivo("spam").build();
        when(denunciaService.resolver("d-1", admin, dto)).thenReturn(resolvida);

        adminService.resolverDenuncia("d-1", admin, dto);

        verify(usuarioService, never()).banir(anyString(), anyString());
    }

    @Test
    void resolverDenuncia_baneUsuarioAlvoQuandoAprovadaEMarcada() {
        DenunciaResolucaoDTO dto = new DenunciaResolucaoDTO();
        dto.setAprovar(true);
        dto.setBanirUsuarioAlvo(true);
        DenunciaResponseDTO resolvida = DenunciaResponseDTO.builder()
                .id("d-1").status(StatusDenuncia.APROVADA).usuarioAlvoId("usr-alvo").motivo("spam").build();
        when(denunciaService.resolver("d-1", admin, dto)).thenReturn(resolvida);

        adminService.resolverDenuncia("d-1", admin, dto);

        verify(usuarioService).banir(eq("usr-alvo"), anyString());
        verify(auditoriaService).registrar(eq(admin), eq(AcaoAuditoria.ADMIN_USUARIO_BANIDO), eq("Usuario"),
                eq("usr-alvo"), anyString());
    }

    @Test
    void resolverDenuncia_naoBaneQuandoDenunciaFoiRejeitada() {
        DenunciaResolucaoDTO dto = new DenunciaResolucaoDTO();
        dto.setAprovar(false);
        dto.setBanirUsuarioAlvo(true);
        DenunciaResponseDTO resolvida = DenunciaResponseDTO.builder()
                .id("d-1").status(StatusDenuncia.REJEITADA).usuarioAlvoId("usr-alvo").build();
        when(denunciaService.resolver("d-1", admin, dto)).thenReturn(resolvida);

        adminService.resolverDenuncia("d-1", admin, dto);

        verify(usuarioService, never()).banir(anyString(), anyString());
    }

    @Test
    void exportarAuditoriaCsv_escapaCamposComPontoEVirgula() {
        LogAuditoria log = LogAuditoria.builder()
                .id("log-1").usuarioId("usr-1").acao(AcaoAuditoria.DENUNCIA_CRIADA)
                .entidadeTipo("Denuncia").entidadeId("d-1")
                .detalhes("motivo; com ponto e vírgula").ip("127.0.0.1")
                .criadoEm(LocalDateTime.of(2026, 1, 1, 12, 0)).build();
        when(logAuditoriaRepository.buscarTodosComFiltros(null, null)).thenReturn(List.of(log));
        Usuario autor = Usuario.builder().id("usr-1").nome("Maria").build();
        when(usuarioRepository.findAllById(List.of("usr-1"))).thenReturn(List.of(autor));

        byte[] csv = adminService.exportarAuditoriaCsv(null, null);
        String conteudo = new String(csv, java.nio.charset.StandardCharsets.UTF_8);

        assertThat(conteudo).contains("\"motivo; com ponto e vírgula\"");
        assertThat(conteudo).contains("Data;UsuarioId;UsuarioNome;Acao;Entidade;EntidadeId;Detalhes;IP");
        assertThat(conteudo).contains("Maria");
    }

    @Test
    void exportarAuditoriaCsv_usaUsuarioRemovidoQuandoAutorNaoExisteMais() {
        LogAuditoria log = LogAuditoria.builder()
                .id("log-2").usuarioId("usr-excluido").acao(AcaoAuditoria.ADMIN_USUARIO_REMOVIDO)
                .entidadeTipo("Usuario").entidadeId("usr-excluido")
                .criadoEm(LocalDateTime.of(2026, 1, 1, 12, 0)).build();
        when(logAuditoriaRepository.buscarTodosComFiltros(null, null)).thenReturn(List.of(log));
        when(usuarioRepository.findAllById(List.of("usr-excluido"))).thenReturn(List.of());

        byte[] csv = adminService.exportarAuditoriaCsv(null, null);
        String conteudo = new String(csv, java.nio.charset.StandardCharsets.UTF_8);

        assertThat(conteudo).contains("Usuário removido");
    }

    @Test
    void listarAuditoria_resolveNomeDoUsuarioResponsavelPelaAcao() {
        LogAuditoria log = LogAuditoria.builder()
                .id("log-1").usuarioId("usr-1").acao(AcaoAuditoria.LOGIN_SUCESSO)
                .criadoEm(LocalDateTime.now()).build();
        Usuario autor = Usuario.builder().id("usr-1").nome("Maria").build();
        when(logAuditoriaRepository.buscarComFiltros(any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(log)));
        when(usuarioRepository.findAllById(List.of("usr-1"))).thenReturn(List.of(autor));

        var resultado = adminService.listarAuditoria(null, null, 0, 20);

        assertThat(resultado.getContent().get(0).getUsuarioNome()).isEqualTo("Maria");
        assertThat(resultado.getContent().get(0).getUsuarioId()).isEqualTo("usr-1");
    }

    @Test
    void obterResumoDashboard_agregaContadoresDeUsuariosEFila() {
        when(usuarioRepository.countByRole(anyString())).thenReturn(5L);
        when(filaEsperaRepository.somarPessoasPorStatus(any())).thenReturn(12L);
        when(logAuditoriaRepository.findTop20ByOrderByCriadoEmDesc()).thenReturn(List.of());
        when(logAuditoriaRepository.contarAcoesPorDia(any())).thenReturn(List.of());
        when(estabelecimentoRepository.countByAberto(true)).thenReturn(3L);
        when(estabelecimentoRepository.countByAberto(false)).thenReturn(7L);
        when(usuarioRepository.contarNovosPorDia(any())).thenReturn(List.of());
        when(estabelecimentoRepository.contarNovosPorDia(any())).thenReturn(List.of());

        var resultado = adminService.obterResumoDashboard();

        assertThat(resultado.getPessoasNaFilaAgora()).isEqualTo(12L);
        assertThat(resultado.getEstabelecimentosAbertos()).isEqualTo(3L);
        assertThat(resultado.getEstabelecimentosFechados()).isEqualTo(7L);
    }
}
