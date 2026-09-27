package com.nowait.service;

import com.nowait.dto.LoginRequestDTO;
import com.nowait.dto.LoginResponseDTO;
import com.nowait.dto.UsuarioRegistroDTO;
import com.nowait.exception.DuplicateResourceException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.Usuario;
import com.nowait.repository.UsuarioRepository;
import com.nowait.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ENTIDADE_USUARIO = "Usuario";

    private final UsuarioRepository     usuarioRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditoriaService      auditoriaService;

    public LoginResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail()).orElse(null);
        if (usuario == null) {
            auditoriaService.registrar((String) null, AcaoAuditoria.LOGIN_FALHA, ENTIDADE_USUARIO, null,
                    "tentativa de login com e-mail não cadastrado");
            throw new ResourceNotFoundException("Usuário", "e-mail", dto.getEmail());
        }

        try {
            authenticateCredentials(dto.getEmail(), dto.getSenha());
        } catch (AuthenticationException e) {
            auditoriaService.registrar(usuario, AcaoAuditoria.LOGIN_FALHA, ENTIDADE_USUARIO, usuario.getId(),
                    "senha incorreta");
            throw e;
        }

        auditoriaService.registrar(usuario, AcaoAuditoria.LOGIN_SUCESSO, ENTIDADE_USUARIO, usuario.getId(), null);
        log.info("Login bem-sucedido: email={}", dto.getEmail());
        return buildLoginResponse(usuario);
    }

    public LoginResponseDTO register(UsuarioRegistroDTO dto) {
        validateEmailNotInUse(dto.getEmail());

        Usuario usuario = buildNewUsuario(dto);
        usuario = usuarioRepository.save(usuario);

        auditoriaService.registrar(usuario, AcaoAuditoria.REGISTRO_CONTA, ENTIDADE_USUARIO, usuario.getId(),
                "role=" + dto.getRole() + ", termos aceitos");
        log.info("Novo usuário registrado: email={} role={}", dto.getEmail(), dto.getRole());
        return buildLoginResponse(usuario);
    }

    private void authenticateCredentials(String email, String senha) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, senha));
    }

    private void validateEmailNotInUse(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Usuário", "e-mail", email);
        }
    }

    private Usuario buildNewUsuario(UsuarioRegistroDTO dto) {
        return Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senhaHash(passwordEncoder.encode(dto.getSenha()))
                .role(dto.getRole())
                .build();
    }

    private LoginResponseDTO buildLoginResponse(Usuario usuario) {
        String token = jwtService.generateToken(usuario);
        return LoginResponseDTO.builder()
                .token(token).type("Bearer")
                .usuarioId(usuario.getId())
                .nome(usuario.getNome()).email(usuario.getEmail()).role(usuario.getRole())
                .build();
    }
}
