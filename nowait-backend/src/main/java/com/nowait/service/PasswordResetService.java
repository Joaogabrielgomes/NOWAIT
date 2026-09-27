package com.nowait.service;

import com.nowait.dto.ResetPasswordDTO;
import com.nowait.exception.InvalidInputException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.PasswordResetToken;
import com.nowait.model.Usuario;
import com.nowait.repository.PasswordResetTokenRepository;
import com.nowait.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_VALIDADE_HORAS = 1;
    private static final String ENTIDADE_USUARIO = "Usuario";

    private final PasswordResetTokenRepository tokenRepository;
    private final UsuarioRepository            usuarioRepository;
    private final EmailService                 emailService;
    private final PasswordEncoder              passwordEncoder;
    private final AuditoriaService             auditoriaService;

    @Transactional
    public void solicitarResetSenha(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            invalidarTokensAnteriores(usuario.getId());
            String token = salvarNovoToken(usuario);
            emailService.enviarEmailResetSenha(email, token);
            auditoriaService.registrar(usuario, AcaoAuditoria.SENHA_RESET_SOLICITADO, ENTIDADE_USUARIO,
                    usuario.getId(), null);
        });
    }

    @Transactional
    public void redefinirSenha(ResetPasswordDTO dto) {
        PasswordResetToken token = buscarTokenValidoOuLancar(dto.getToken());
        atualizarSenha(token.getUsuario(), dto.getNovaSenha());
        marcarTokenComoUsado(token);
        auditoriaService.registrar(token.getUsuario(), AcaoAuditoria.SENHA_REDEFINIDA, ENTIDADE_USUARIO,
                token.getUsuario().getId(), null);
    }

    private void invalidarTokensAnteriores(String usuarioId) {
        tokenRepository.deleteByUsuarioId(usuarioId);
    }

    private String salvarNovoToken(Usuario usuario) {
        String tokenValue = UUID.randomUUID().toString();

        tokenRepository.saveAndFlush(PasswordResetToken.builder()
                .usuario(usuario)
                .token(tokenValue)
                .expiraEm(LocalDateTime.now().plusHours(TOKEN_VALIDADE_HORAS))
                .usado(false)
                .build());

        return tokenValue;
    }

    private PasswordResetToken buscarTokenValidoOuLancar(String tokenValue) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidInputException("Token inválido ou inexistente"));

        if (token.estaExpirado()) {
            throw new InvalidInputException("Token expirado — solicite um novo link");
        }
        if (token.isUsado()) {
            throw new InvalidInputException("Este token já foi utilizado");
        }

        return token;
    }

    private void atualizarSenha(Usuario usuario, String novaSenha) {
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    private void marcarTokenComoUsado(PasswordResetToken token) {
        token.setUsado(true);
        tokenRepository.save(token);
    }
}
