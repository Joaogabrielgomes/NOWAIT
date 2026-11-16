package com.nowait.service;

import com.nowait.exception.BusinessRuleException;
import com.nowait.model.Mesa;
import com.nowait.model.StatusMesa;
import com.nowait.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlocacaoMesaService {

    private final MesaRepository mesaRepository;

    @Transactional
    public Optional<Mesa> alocarMesa(String estabelecimentoId, int quantidadePessoas, String statusDestino) {
        return buscarELockarMesaDisponivel(estabelecimentoId, quantidadePessoas, statusDestino);
    }

    @Transactional
    public Optional<Mesa> alocarMesa(String estabelecimentoId, int quantidadePessoas) {
        return buscarELockarMesaDisponivel(estabelecimentoId, quantidadePessoas, StatusMesa.OCUPADA);
    }

    private Optional<Mesa> buscarELockarMesaDisponivel(String estabelecimentoId, int quantidadePessoas, String statusDestino) {
        List<Mesa> livresOrdenadasPorCapacidade;
        try {
            livresOrdenadasPorCapacidade =
                    mesaRepository.buscarLivresParaAlocacaoComLock(estabelecimentoId, StatusMesa.LIVRE);
        } catch (PessimisticLockingFailureException ex) {
            throw new BusinessRuleException(
                    "Não foi possível alocar uma mesa agora, outra operação está em andamento. Tente novamente em instantes.");
        }

        Optional<Mesa> mesaEscolhida = livresOrdenadasPorCapacidade.stream()
                .filter(mesa -> mesa.getCapacidade() >= quantidadePessoas)
                .findFirst();

        mesaEscolhida.ifPresent(mesa -> {
            mesa.setStatus(statusDestino);
            mesaRepository.save(mesa);
        });

        return mesaEscolhida;
    }
}
