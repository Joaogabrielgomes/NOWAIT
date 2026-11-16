package com.nowait.service;

import com.nowait.model.AcaoAuditoria;
import com.nowait.model.LogAuditoria;
import com.nowait.model.Usuario;
import com.nowait.repository.LogAuditoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes do AuditoriaService: registro de log (a partir de um Usuario ou direto do id),
 * tolerância a falhas (uma exceção ao salvar não deve propagar e derrubar a ação que estava
 * sendo auditada) e a limpeza periódica de logs antigos.
 */
@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock private LogAuditoriaRepository logAuditoriaRepository;

    @InjectMocks
    private AuditoriaService auditoriaService;

    @Test
    void registrar_persisteLogComDadosDoUsuario() {
        Usuario usuario = Usuario.builder().id("usr-1").build();

        auditoriaService.registrar(usuario, AcaoAuditoria.LOGIN_SUCESSO, "Usuario", "usr-1", "detalhe");

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logAuditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuarioId()).isEqualTo("usr-1");
        assertThat(captor.getValue().getAcao()).isEqualTo(AcaoAuditoria.LOGIN_SUCESSO);
    }

    @Test
    void registrar_aceitaUsuarioNuloParaAcoesNaoAutenticadas() {
        auditoriaService.registrar((Usuario) null, AcaoAuditoria.LOGIN_FALHA, "Usuario", null, "e-mail inexistente");

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logAuditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuarioId()).isNull();
    }

    @Test
    void registrar_naoPropagaExcecaoQuandoRepositorioFalha() {
        when(logAuditoriaRepository.save(any())).thenThrow(new RuntimeException("falha no banco"));

        assertThatCode(() -> auditoriaService.registrar("usr-1", AcaoAuditoria.LOGIN_SUCESSO, "Usuario", "usr-1", null))
                .doesNotThrowAnyException();
    }

    @Test
    void limparLogsAntigos_removeLogsComMaisDeDozeMeses() {
        when(logAuditoriaRepository.deleteByCriadoEmBefore(any(LocalDateTime.class))).thenReturn(3L);

        auditoriaService.limparLogsAntigos();

        verify(logAuditoriaRepository).deleteByCriadoEmBefore(any(LocalDateTime.class));
    }
}
