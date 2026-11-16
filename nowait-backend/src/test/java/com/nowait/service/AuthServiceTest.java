package com.nowait.service;

import com.nowait.dto.LoginRequestDTO;
import com.nowait.dto.LoginResponseDTO;
import com.nowait.dto.UsuarioRegistroDTO;
import com.nowait.exception.ConteudoImproprioException;
import com.nowait.exception.DuplicateResourceException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.Perfil;
import com.nowait.model.Usuario;
import com.nowait.repository.UsuarioRepository;
import com.nowait.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes de autenticação: login (sucesso, e-mail inexistente, senha incorreta, conta banida)
 * e registro (sucesso, e-mail duplicado, nome com conteúdo impróprio).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuditoriaService auditoriaService;
    @Mock private ModeracaoService moderacaoService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id("usr-1")
                .nome("Maria Cliente")
                .email("maria@exemplo.com")
                .senhaHash("hash-qualquer")
                .role(Perfil.CLIENTE)
                .banido(false)
                .build();
    }

    @Test
    void login_comEmailInexistente_lancaResourceNotFoundException() {
        when(usuarioRepository.findByEmail("inexistente@exemplo.com")).thenReturn(Optional.empty());

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("inexistente@exemplo.com");
        dto.setSenha("qualquer");

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(auditoriaService).registrar((String) null, AcaoAuditoria.LOGIN_FALHA, "Usuario", null,
                "tentativa de login com e-mail não cadastrado");
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void login_comSenhaIncorreta_propagaExcecaoERegistraAuditoria() {
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("senha inválida"));

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail(usuario.getEmail());
        dto.setSenha("senha-errada");

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(BadCredentialsException.class);

        verify(auditoriaService).registrar(usuario, AcaoAuditoria.LOGIN_FALHA, "Usuario", usuario.getId(),
                "senha incorreta");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_comContaBanida_propagaDisabledExceptionERegistraAuditoria() {
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("conta desabilitada"));

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail(usuario.getEmail());
        dto.setSenha("qualquer");

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(DisabledException.class);

        verify(auditoriaService).registrar(usuario, AcaoAuditoria.LOGIN_FALHA, "Usuario", usuario.getId(),
                "conta banida");
    }

    @Test
    void login_comCredenciaisValidas_retornaTokenERegistraSucesso() {
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(jwtService.generateToken(usuario)).thenReturn("token-fake-123");

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail(usuario.getEmail());
        dto.setSenha("senha-correta");

        LoginResponseDTO resposta = authService.login(dto);

        assertThat(resposta.getToken()).isEqualTo("token-fake-123");
        assertThat(resposta.getUsuarioId()).isEqualTo(usuario.getId());
        assertThat(resposta.getRole()).isEqualTo(Perfil.CLIENTE);
        verify(auditoriaService).registrar(usuario, AcaoAuditoria.LOGIN_SUCESSO, "Usuario", usuario.getId(), null);
    }

    @Test
    void register_comEmailJaCadastrado_lancaDuplicateResourceExceptionENaoSalva() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(true);

        UsuarioRegistroDTO dto = new UsuarioRegistroDTO();
        dto.setNome("Novo Usuário");
        dto.setEmail(usuario.getEmail());
        dto.setSenha("Senha@123");
        dto.setRole(Perfil.CLIENTE);
        dto.setAceiteTermos(true);

        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(DuplicateResourceException.class);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(moderacaoService);
    }

    @Test
    void register_comNomeImproprio_propagaExcecaoDaModeracaoENaoSalva() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        doThrow(new ConteudoImproprioException("nome de usuário contém termo não permitido"))
                .when(moderacaoService).verificar(eq("Nome Ofensivo"), eq("nome de usuário"));

        UsuarioRegistroDTO dto = new UsuarioRegistroDTO();
        dto.setNome("Nome Ofensivo");
        dto.setEmail("novo@exemplo.com");
        dto.setSenha("Senha@123");
        dto.setRole(Perfil.CLIENTE);
        dto.setAceiteTermos(true);

        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(ConteudoImproprioException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void register_comDadosValidos_criaUsuarioComSenhaCriptografadaERetornaToken() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash-gerado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId("usr-novo");
            return u;
        });
        when(jwtService.generateToken(any())).thenReturn("token-novo-usuario");

        UsuarioRegistroDTO dto = new UsuarioRegistroDTO();
        dto.setNome("Novo Cliente");
        dto.setEmail("novo@exemplo.com");
        dto.setSenha("Senha@123");
        dto.setRole(Perfil.CLIENTE);
        dto.setAceiteTermos(true);

        LoginResponseDTO resposta = authService.register(dto);

        assertThat(resposta.getToken()).isEqualTo("token-novo-usuario");
        assertThat(resposta.getEmail()).isEqualTo("novo@exemplo.com");
        verify(moderacaoService).verificar("Novo Cliente", "nome de usuário");
        verify(auditoriaService).registrar(any(Usuario.class), eq(AcaoAuditoria.REGISTRO_CONTA),
                eq("Usuario"), eq("usr-novo"), anyString());
    }
}
