package com.nowait.security;

import com.nowait.model.Perfil;
import com.nowait.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes do serviço de JWT: geração de token, extração do username (subject) e validação
 * (token válido para o usuário certo, inválido para outro usuário, e expiração).
 */
class JwtServiceTest {

    private JwtService jwtService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Chave HMAC-SHA precisa de pelo menos 256 bits (32 caracteres) — valor só para teste.
        ReflectionTestUtils.setField(jwtService, "secret", "chave-de-teste-com-pelo-menos-32-caracteres!!");
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);

        usuario = Usuario.builder().id("usr-1").nome("Maria").email("maria@example.com")
                .senhaHash("hash").role(Perfil.CLIENTE).build();
    }

    @Test
    void generateToken_eExtractUsername_devolveOEmailDoUsuario() {
        String token = jwtService.generateToken(usuario);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("maria@example.com");
    }

    @Test
    void isTokenValid_retornaTrueParaOMesmoUsuario() {
        String token = jwtService.generateToken(usuario);

        assertThat(jwtService.isTokenValid(token, usuario)).isTrue();
    }

    @Test
    void isTokenValid_retornaFalseParaUsuarioDiferente() {
        String token = jwtService.generateToken(usuario);
        Usuario outro = Usuario.builder().id("usr-2").email("outro@example.com")
                .senhaHash("hash").role(Perfil.CLIENTE).build();

        assertThat(jwtService.isTokenValid(token, outro)).isFalse();
    }

    @Test
    void isTokenValid_lancaExcecaoQuandoTokenJaExpirou() {
        // A biblioteca JWT rejeita o token expirado já na etapa de parsing (ExpiredJwtException),
        // antes mesmo da checagem explícita de expiração em isTokenExpired().
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.generateToken(usuario);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, usuario))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}
