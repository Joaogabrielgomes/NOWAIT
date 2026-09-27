package com.nowait.service;

import com.nowait.model.Mesa;
import com.nowait.model.StatusMesa;
import com.nowait.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlocacaoMesaService {

    private final MesaRepository mesaRepository;

    public Optional<Mesa> alocarMesa(String estabelecimentoId, int quantidadePessoas) {
        List<Mesa> livresOrdenadasPorCapacidade =
                mesaRepository.findByEstabelecimentoIdAndStatusOrderByCapacidadeAsc(estabelecimentoId, StatusMesa.LIVRE);

        return livresOrdenadasPorCapacidade.stream()
                .filter(mesa -> mesa.getCapacidade() >= quantidadePessoas)
                .findFirst();
    }
}
