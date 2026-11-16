package com.nowait.service;

import com.nowait.dto.ResetPasswordDTO;
import com.nowait.exception.InvalidInputException;
import com.nowait.model.PasswordResetToken;
import com.nowait.model.Usuario;
import com.nowait.repository.PasswordResetTokenRepository;
import com.nowait.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes do fluxo de redefinição de senha: solicitação (silenciosa quando o e-mail não existe,
 * para não revelar quais e-mails estão cadastrados) e redefinição (token inválido, expirado,
 * já usado, e o caminho de sucesso).
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id("usr-1").nome("Maria").email("maria@example.com").build();
    }

    @Test
    void solicitarResetSenha_naoFazNadaQuandoEmailNaoExiste() {
        when(usuarioRepository.findByEmail("fantasma@example.com")).thenReturn(Optional.empty());

        passwordResetService.solicitarResetSenha("fantasma@example.com");

        verify(emailService, never()).enviarEmailResetSenha(anyString(), anyString());
        verify(tokenRepository, never()).saveAndFlush(any());
    }

    @Test
    void solicitarResetSenha_invalidaTokensAnterioresEEnviaNovoEmail() {
        when(usuarioRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(usuario));

        passwordResetService.solicitarResetSenha("maria@example.com");

        verify(tokenRepository).deleteByUsuarioId("usr-1");
        verify(tokenRepository).saveAndFlush(any(PasswordResetToken.class));
        verify(emailService).enviarEmailResetSenha(eq("maria@example.com"), anyString());
    }

    @Test
    void redefinirSenha_lancaExcecaoQuandoTokenInexistente() {
        when(tokenRepository.findByToken("token-x")).thenReturn(Optional.empty());
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken("token-x");
        dto.setNovaSenha("Nova@123");

        assertThatThrownBy(() -> passwordResetService.redefinirSenha(dto))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void redefinirSenha_lancaExcecaoQuandoTokenExpirado() {
        PasswordResetToken token = PasswordResetToken.builder().id("t-1").usuario(usuario).token("token-x")
                .expiraEm(LocalDateTime.now().minusMinutes(5)).usado(false).build();
        when(tokenRepository.findByToken("token-x")).thenReturn(Optional.of(token));
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken("token-x");
        dto.setNovaSenha("Nova@123");

        assertThatThrownBy(() -> passwordResetService.redefinirSenha(dto))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void redefinirSenha_lancaExcecaoQuandoTokenJaUsado() {
        PasswordResetToken token = PasswordResetToken.builder().id("t-1").usuario(usuario).token("token-x")
                .expiraEm(LocalDateTime.now().plusMinutes(30)).usado(true).build();
        when(tokenRepository.findByToken("token-x")).thenReturn(Optional.of(token));
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken("token-x");
        dto.setNovaSenha("Nova@123");

        assertThatThrownBy(() -> passwordResetService.redefinirSenha(dto))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void redefinirSenha_atualizaSenhaEMarcaTokenComoUsado() {
        PasswordResetToken token = PasswordResetToken.builder().id("t-1").usuario(usuario).token("token-x")
                .expiraEm(LocalDateTime.now().plusMinutes(30)).usado(false).build();
        when(tokenRepository.findByToken("token-x")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("Nova@123")).thenReturn("hash-nova");
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken("token-x");
        dto.setNovaSenha("Nova@123");

        passwordResetService.redefinirSenha(dto);

        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).save(token);
        org.assertj.core.api.Assertions.assertThat(usuario.getSenhaHash()).isEqualTo("hash-nova");
        org.assertj.core.api.Assertions.assertThat(token.isUsado()).isTrue();
    }
}
