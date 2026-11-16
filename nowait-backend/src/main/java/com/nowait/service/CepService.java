package com.nowait.service;

import com.nowait.dto.CepResponseDTO;
import com.nowait.dto.EnderecoDTO;
import com.nowait.exception.CepNaoEncontradoException;
import com.nowait.exception.CepServiceIndisponivelException;
import com.nowait.exception.InvalidInputException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CepService {

    private final RestTemplate restTemplate;

    @Value("${viacep.base-url}")
    private String baseUrl;

    public EnderecoDTO buscarEndereco(String cepBruto) {
        String cep = normalizar(cepBruto);

        CepResponseDTO resposta = consultarViaCep(cep);

        if (resposta == null || resposta.isErro()) {
            throw new CepNaoEncontradoException(cep);
        }

        return EnderecoDTO.builder()
                .cep(cep)
                .logradouro(resposta.getLogradouro())
                .bairro(resposta.getBairro())
                .cidade(resposta.getLocalidade())
                .estado(resposta.getUf())
                .build();
    }

    private CepResponseDTO consultarViaCep(String cep) {
        try {
            String url = String.format("%s/%s/json/", baseUrl, cep);
            return restTemplate.getForObject(url, CepResponseDTO.class);
        } catch (RestClientException e) {
            throw new CepServiceIndisponivelException("Falha ao consultar ViaCEP para o CEP " + cep, e);
        }
    }

    private String normalizar(String cepBruto) {
        if (cepBruto == null) {
            throw new InvalidInputException("CEP é obrigatório");
        }
        String apenasDigitos = cepBruto.replaceAll("\\D", "");
        if (apenasDigitos.length() != 8) {
            throw new InvalidInputException("CEP inválido: " + cepBruto);
        }
        return apenasDigitos;
    }
}
