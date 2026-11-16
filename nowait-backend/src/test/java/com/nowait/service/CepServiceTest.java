package com.nowait.service;

import com.nowait.dto.CepResponseDTO;
import com.nowait.dto.EnderecoDTO;
import com.nowait.exception.CepNaoEncontradoException;
import com.nowait.exception.CepServiceIndisponivelException;
import com.nowait.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Testes da integração com o ViaCEP: normalização do CEP informado (removendo máscara),
 * validação de formato, tratamento de "erro: true" (CEP inexistente) e de falha de rede
 * do serviço externo.
 */
@ExtendWith(MockitoExtension.class)
class CepServiceTest {

    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private CepService cepService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cepService, "baseUrl", "https://viacep.com.br/ws");
    }

    @Test
    void buscarEndereco_lancaExcecaoQuandoCepNulo() {
        assertThatThrownBy(() -> cepService.buscarEndereco(null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void buscarEndereco_lancaExcecaoQuandoCepComQuantidadeDeDigitosInvalida() {
        assertThatThrownBy(() -> cepService.buscarEndereco("123"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void buscarEndereco_removeMascaraAntesDeConsultar() {
        CepResponseDTO resposta = new CepResponseDTO();
        resposta.setLogradouro("Rua X");
        resposta.setBairro("Centro");
        resposta.setLocalidade("Mogi das Cruzes");
        resposta.setUf("SP");
        when(restTemplate.getForObject(anyString(), eq(CepResponseDTO.class))).thenReturn(resposta);

        EnderecoDTO resultado = cepService.buscarEndereco("08773-000");

        assertThat(resultado.getCep()).isEqualTo("08773000");
        assertThat(resultado.getCidade()).isEqualTo("Mogi das Cruzes");
    }

    @Test
    void buscarEndereco_lancaExcecaoQuandoViaCepRetornaErro() {
        CepResponseDTO resposta = new CepResponseDTO();
        resposta.setErro(true);
        when(restTemplate.getForObject(anyString(), eq(CepResponseDTO.class))).thenReturn(resposta);

        assertThatThrownBy(() -> cepService.buscarEndereco("00000000"))
                .isInstanceOf(CepNaoEncontradoException.class);
    }

    @Test
    void buscarEndereco_lancaExcecaoQuandoViaCepRetornaNulo() {
        when(restTemplate.getForObject(anyString(), eq(CepResponseDTO.class))).thenReturn(null);

        assertThatThrownBy(() -> cepService.buscarEndereco("08773000"))
                .isInstanceOf(CepNaoEncontradoException.class);
    }

    @Test
    void buscarEndereco_lancaExcecaoQuandoServicoExternoFalha() {
        when(restTemplate.getForObject(anyString(), eq(CepResponseDTO.class)))
                .thenThrow(new RestClientException("timeout"));

        assertThatThrownBy(() -> cepService.buscarEndereco("08773000"))
                .isInstanceOf(CepServiceIndisponivelException.class);
    }
}
