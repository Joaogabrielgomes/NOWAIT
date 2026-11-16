package com.nowait.service;

import com.nowait.dto.ContestacaoBanimentoInfoDTO;
import com.nowait.dto.ContestacaoBanimentoRequestDTO;
import com.nowait.dto.ContestacaoBanimentoResponseDTO;
import com.nowait.exception.InvalidInputException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.ContestacaoBanimento;
import com.nowait.model.StatusContestacao;
import com.nowait.model.Usuario;
import com.nowait.repository.ContestacaoBanimentoRepository;
import com.nowait.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes do fluxo de contestação de banimento: consulta por token (sempre não autenticada,
 * já que o usuário banido não consegue logar), registro da contestação e marcação como
 * analisada pelo admin.
 */
@ExtendWith(MockitoExtension.class)
class ContestacaoBanimentoServiceTest {

    @Mock private ContestacaoBanimentoRepository contestacaoBanimentoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private ContestacaoBanimentoService contestacaoBanimentoService;

    private Usuario banido;

    @BeforeEach
    void setUp() {
        banido = Usuario.builder().id("usr-1").nome("Fulano").banido(true)
                .motivoBanimento("Spam").tokenContestacao("token-123").build();
    }

    @Test
    void buscarInfoPorToken_lancaExcecaoQuandoTokenInvalido() {
        when(usuarioRepository.findByTokenContestacaoAndBanidoTrue("token-invalido"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> contestacaoBanimentoService.buscarInfoPorToken("token-invalido"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void buscarInfoPorToken_retornaDadosDoUsuarioBanido() {
        when(usuarioRepository.findByTokenContestacaoAndBanidoTrue("token-123"))
                .thenReturn(Optional.of(banido));

        ContestacaoBanimentoInfoDTO resultado = contestacaoBanimentoService.buscarInfoPorToken("token-123");

        assertThat(resultado.getUsuarioNome()).isEqualTo("Fulano");
        assertThat(resultado.getMotivoBanimento()).isEqualTo("Spam");
    }

    @Test
    void registrar_persisteContestacaoEAuditoria() {
        when(usuarioRepository.findByTokenContestacaoAndBanidoTrue("token-123"))
                .thenReturn(Optional.of(banido));
        when(contestacaoBanimentoRepository.save(any(ContestacaoBanimento.class))).thenAnswer(inv -> {
            ContestacaoBanimento c = inv.getArgument(0);
            c.setId("contestacao-1");
            return c;
        });

        ContestacaoBanimentoRequestDTO dto = new ContestacaoBanimentoRequestDTO();
        dto.setToken("token-123");
        dto.setMensagem("Não fiz nada de errado");

        contestacaoBanimentoService.registrar(dto);

        verify(contestacaoBanimentoRepository).save(any(ContestacaoBanimento.class));
        verify(auditoriaService).registrar(eq(banido), any(), eq("ContestacaoBanimento"), eq("contestacao-1"), isNull());
    }

    @Test
    void marcarAnalisada_lancaExcecaoQuandoNaoEncontrada() {
        when(contestacaoBanimentoRepository.findById("x")).thenReturn(Optional.empty());
        Usuario admin = Usuario.builder().id("admin-1").build();

        assertThatThrownBy(() -> contestacaoBanimentoService.marcarAnalisada("x", admin))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void marcarAnalisada_atualizaStatusParaAnalisada() {
        ContestacaoBanimento contestacao = ContestacaoBanimento.builder()
                .id("c-1").usuario(banido).mensagem("teste").status(StatusContestacao.PENDENTE).build();
        when(contestacaoBanimentoRepository.findById("c-1")).thenReturn(Optional.of(contestacao));
        when(contestacaoBanimentoRepository.save(any(ContestacaoBanimento.class))).thenAnswer(inv -> inv.getArgument(0));
        Usuario admin = Usuario.builder().id("admin-1").build();

        ContestacaoBanimentoResponseDTO resultado = contestacaoBanimentoService.marcarAnalisada("c-1", admin);

        assertThat(resultado.getStatus()).isEqualTo(StatusContestacao.ANALISADA);
    }
}
