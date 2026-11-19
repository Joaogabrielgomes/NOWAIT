package com.nowait.service;

import com.nowait.dto.UsuarioAtualizacaoDTO;
import com.nowait.dto.UsuarioResponseDTO;
import com.nowait.exception.DuplicateResourceException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.Perfil;
import com.nowait.model.Usuario;
import com.nowait.repository.AgendamentoRepository;
import com.nowait.repository.EstabelecimentoRepository;
import com.nowait.repository.FilaEsperaRepository;
import com.nowait.repository.PasswordResetTokenRepository;
import com.nowait.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes do UsuarioService: busca, banimento/desbanimento (com disparo de e-mail), atualização
 * de perfil e exclusão da própria conta (exclusão real vs. anonimização quando há histórico
 * vinculado — fila, agendamento ou estabelecimento).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstabelecimentoRepository estabelecimentoRepository;
    @Mock private FilaEsperaRepository filaEsperaRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditoriaService auditoriaService;
    @Mock private EstabelecimentoService estabelecimentoService;
    @Mock private EmailService emailService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id("usr-1")
                .nome("Maria")
                .email("maria@example.com")
                .senhaHash("hash")
                .role(Perfil.CLIENTE)
                .build();
    }

    @Test
    void loadUserByUsername_retornaUsuarioQuandoExiste() {
        when(usuarioRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(usuario));

        UserDetails resultado = usuarioService.loadUserByUsername("maria@example.com");

        assertThat(resultado).isEqualTo(usuario);
    }

    @Test
    void loadUserByUsername_lancaExcecaoQuandoNaoExiste() {
        when(usuarioRepository.findByEmail("fantasma@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.loadUserByUsername("fantasma@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void buscarPorId_lancaExcecaoQuandoNaoEncontrado() {
        when(usuarioRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorId("x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void banir_marcaBanidoGeraTokenEEnviaEmail() {
        when(usuarioRepository.findById("usr-1")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> {
            Runnable r = inv.getArgument(0);
            r.run();
            return null;
        }).when(emailService).enviarComSeguranca(any());

        UsuarioResponseDTO resultado = usuarioService.banir("usr-1", "Comportamento inadequado");

        assertThat(resultado.isBanido()).isTrue();
        assertThat(usuario.isBanido()).isTrue();
        assertThat(usuario.getMotivoBanimento()).isEqualTo("Comportamento inadequado");
        assertThat(usuario.getTokenContestacao()).isNotBlank();
        verify(emailService).enviarEmailBanimento(eq(usuario.getEmail()), eq(usuario.getNome()), eq("Comportamento inadequado"), anyString());
    }

    @Test
    void desbanir_limpaBanimentoEEnviaEmail() {
        usuario.setBanido(true);
        usuario.setMotivoBanimento("motivo antigo");
        usuario.setTokenContestacao("token-antigo");
        when(usuarioRepository.findById("usr-1")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> {
            Runnable r = inv.getArgument(0);
            r.run();
            return null;
        }).when(emailService).enviarComSeguranca(any());

        UsuarioResponseDTO resultado = usuarioService.desbanir("usr-1");

        assertThat(resultado.isBanido()).isFalse();
        assertThat(usuario.getMotivoBanimento()).isNull();
        assertThat(usuario.getTokenContestacao()).isNull();
        verify(emailService).enviarEmailDesbanimento(usuario.getEmail(), usuario.getNome());
    }

    @Test
    void atualizarPerfil_lancaExcecaoQuandoEmailJaUsadoPorOutroUsuario() {
        UsuarioAtualizacaoDTO dto = new UsuarioAtualizacaoDTO();
        dto.setNome("Maria Nova");
        dto.setEmail("outro@example.com");
        when(usuarioRepository.existsByEmailAndIdNot("outro@example.com", "usr-1")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.atualizarPerfil(usuario, dto))
                .isInstanceOf(DuplicateResourceException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void atualizarPerfil_atualizaNomeEEmailSemTrocarSenhaQuandoNaoInformada() {
        UsuarioAtualizacaoDTO dto = new UsuarioAtualizacaoDTO();
        dto.setNome("Maria Nova");
        dto.setEmail("maria.nova@example.com");
        when(usuarioRepository.existsByEmailAndIdNot(anyString(), anyString())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponseDTO resultado = usuarioService.atualizarPerfil(usuario, dto);

        assertThat(resultado.getNome()).isEqualTo("Maria Nova");
        assertThat(resultado.getEmail()).isEqualTo("maria.nova@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(auditoriaService).registrar(eq(usuario), any(), eq("Usuario"), eq("usr-1"), isNull());
    }

    @Test
    void atualizarPerfil_trocaSenhaQuandoInformada() {
        UsuarioAtualizacaoDTO dto = new UsuarioAtualizacaoDTO();
        dto.setNome("Maria");
        dto.setEmail("maria@example.com");
        dto.setSenha("novaSenha123");
        when(usuarioRepository.existsByEmailAndIdNot(anyString(), anyString())).thenReturn(false);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hash-nova");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.atualizarPerfil(usuario, dto);

        assertThat(usuario.getSenhaHash()).isEqualTo("hash-nova");
    }

    @Test
    void excluirPropriaConta_excluiDeVerdadeQuandoSemHistoricoVinculado() {
        when(estabelecimentoRepository.existsByUsuarioId("usr-1")).thenReturn(false);
        when(filaEsperaRepository.existsByClienteId("usr-1")).thenReturn(false);
        when(agendamentoRepository.existsByClienteId("usr-1")).thenReturn(false);
        when(estabelecimentoRepository.findByUsuarioId("usr-1")).thenReturn(Optional.empty());

        usuarioService.excluirPropriaConta(usuario);

        verify(usuarioRepository).delete(usuario);
        verify(usuarioRepository, never()).save(any());
        verify(auditoriaService).registrar(eq("usr-1"), any(), eq("Usuario"), eq("usr-1"), isNull());
    }

    @Test
    void excluirPropriaConta_anonimizaQuandoPossuiHistoricoDeFila() {
        when(estabelecimentoRepository.existsByUsuarioId("usr-1")).thenReturn(false);
        when(filaEsperaRepository.existsByClienteId("usr-1")).thenReturn(true);
        when(estabelecimentoRepository.findByUsuarioId("usr-1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-aleatorio");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.excluirPropriaConta(usuario);

        verify(usuarioRepository, never()).delete(any(Usuario.class));
        assertThat(usuario.getNome()).isEqualTo("Usuário removido");
        assertThat(usuario.getEmail()).contains("@anonimizado.nowait");
        assertThat(usuario.getCep()).isNull();
    }

    @Test
    void toResponseDTO_mapeiaCamposPrincipais() {
        usuario.setCidade("Mogi das Cruzes");

        UsuarioResponseDTO dto = usuarioService.toResponseDTO(usuario);

        assertThat(dto.getId()).isEqualTo("usr-1");
        assertThat(dto.getNome()).isEqualTo("Maria");
        assertThat(dto.getEndereco().getCidade()).isEqualTo("Mogi das Cruzes");
    }
}
