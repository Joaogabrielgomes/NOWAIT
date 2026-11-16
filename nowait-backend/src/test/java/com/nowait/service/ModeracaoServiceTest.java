package com.nowait.service;

import com.nowait.exception.ConteudoImproprioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Testes do filtro de moderação de texto: carrega a lista real de palavras proibidas
 * (src/main/resources/moderacao/palavras-proibidas.txt) e verifica a detecção com acentos,
 * maiúsculas/minúsculas e textos legítimos que não devem ser bloqueados.
 */
class ModeracaoServiceTest {

    private ModeracaoService moderacaoService;

    @BeforeEach
    void setUp() {
        moderacaoService = new ModeracaoService();
        moderacaoService.carregarPalavras();
    }

    @Test
    void contemPalavraProibida_retornaFalseParaTextoCurto() {
        assertThat(moderacaoService.contemPalavraProibida("ab")).isFalse();
    }

    @Test
    void contemPalavraProibida_retornaFalseParaTextoNuloOuVazio() {
        assertThat(moderacaoService.contemPalavraProibida(null)).isFalse();
        assertThat(moderacaoService.contemPalavraProibida("   ")).isFalse();
    }

    @Test
    void contemPalavraProibida_retornaFalseParaNomeLegitimo() {
        assertThat(moderacaoService.contemPalavraProibida("Restaurante Bom Sabor")).isFalse();
    }

    @Test
    void contemPalavraProibida_detectaPalavraProibidaIgnorandoCaixaEAcento() {
        assertThat(moderacaoService.contemPalavraProibida("Restaurante do Idiota")).isTrue();
        assertThat(moderacaoService.contemPalavraProibida("RESTAURANTE IDIOTA")).isTrue();
    }

    @Test
    void verificar_lancaExcecaoQuandoTextoContemPalavraProibida() {
        assertThatThrownBy(() -> moderacaoService.verificar("Restaurante Idiota", "nome do estabelecimento"))
                .isInstanceOf(ConteudoImproprioException.class);
    }

    @Test
    void verificar_naoLancaExcecaoParaTextoLegitimo() {
        assertThatCode(() -> moderacaoService.verificar("Restaurante Bom Sabor", "nome do estabelecimento"))
                .doesNotThrowAnyException();
    }
}
