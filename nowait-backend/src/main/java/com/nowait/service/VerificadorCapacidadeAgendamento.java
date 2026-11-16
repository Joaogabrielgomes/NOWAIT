package com.nowait.service;

import com.nowait.exception.BusinessRuleException;
import com.nowait.model.Agendamento;
import com.nowait.model.Estabelecimento;
import com.nowait.model.Mesa;
import com.nowait.repository.AgendamentoRepository;
import com.nowait.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VerificadorCapacidadeAgendamento {

    private static final double PERCENTUAL_MAXIMO_POR_HORARIO = 0.5;

    private final AgendamentoRepository agendamentoRepository;
    private final MesaRepository mesaRepository;

    public void validarLimitePorHorario(Estabelecimento estabelecimento, LocalDateTime dataHora,
                                         List<String> statusConsiderados) {
        long totalMesas = mesaRepository.countByEstabelecimentoId(estabelecimento.getId());
        long limitePorHorario = (long) Math.floor(totalMesas * PERCENTUAL_MAXIMO_POR_HORARIO);

        long agendamentosNoHorario = agendamentoRepository.countByEstabelecimentoIdAndStatusInAndDataHora(
                estabelecimento.getId(), statusConsiderados, dataHora);

        if (agendamentosNoHorario >= limitePorHorario) {
            throw new BusinessRuleException(
                    "Este horário já atingiu o limite de mesas reservadas. Escolha outro horário");
        }
    }

    public void validar(Estabelecimento estabelecimento, LocalDateTime dataHora, int quantidadePessoas,
                         List<String> statusConsiderados) {
        int duracaoMinutos = estabelecimento.getTempoMedioAtendimentoMinutos();

        List<Integer> demandas = new ArrayList<>(
                buscarDemandasSobrepostas(estabelecimento.getId(), dataHora, duracaoMinutos, statusConsiderados));
        demandas.add(quantidadePessoas);

        List<Integer> capacidades = mesaRepository.findByEstabelecimentoIdOrderByNumeroAsc(estabelecimento.getId())
                .stream()
                .map(Mesa::getCapacidade)
                .toList();

        if (!cabemTodasAsDemandas(capacidades, demandas)) {
            throw new BusinessRuleException(
                    "Não há mesa com capacidade suficiente disponível para este horário. Tente outro horário");
        }
    }

    private List<Integer> buscarDemandasSobrepostas(String estabelecimentoId, LocalDateTime dataHora,
                                                     int duracaoMinutos, List<String> statusConsiderados) {
        LocalDateTime inicioJanela = dataHora.minusMinutes(duracaoMinutos);
        LocalDateTime fimJanela = dataHora.plusMinutes(duracaoMinutos);

        return agendamentoRepository
                .findByEstabelecimentoIdAndStatusInAndDataHoraBetween(
                        estabelecimentoId, statusConsiderados, inicioJanela, fimJanela)
                .stream()
                .filter(agendamento -> seSobrepoe(agendamento.getDataHora(), dataHora, duracaoMinutos))
                .map(Agendamento::getQuantidadePessoas)
                .toList();
    }

    private boolean seSobrepoe(LocalDateTime inicioExistente, LocalDateTime inicioNovo, int duracaoMinutos) {
        long diferencaMinutos = Math.abs(Duration.between(inicioExistente, inicioNovo).toMinutes());
        return diferencaMinutos < duracaoMinutos;
    }

    private boolean cabemTodasAsDemandas(List<Integer> capacidades, List<Integer> demandas) {
        List<Integer> capacidadesOrdenadas = new ArrayList<>(capacidades);
        Collections.sort(capacidadesOrdenadas);

        List<Integer> demandasOrdenadas = new ArrayList<>(demandas);
        demandasOrdenadas.sort(Collections.reverseOrder());

        boolean[] mesaUsada = new boolean[capacidadesOrdenadas.size()];
        for (int demanda : demandasOrdenadas) {
            int indice = encontrarMenorMesaDisponivel(capacidadesOrdenadas, mesaUsada, demanda);
            if (indice == -1) {
                return false;
            }
            mesaUsada[indice] = true;
        }
        return true;
    }

    private int encontrarMenorMesaDisponivel(List<Integer> capacidadesOrdenadas, boolean[] mesaUsada, int demanda) {
        for (int i = 0; i < capacidadesOrdenadas.size(); i++) {
            if (!mesaUsada[i] && capacidadesOrdenadas.get(i) >= demanda) {
                return i;
            }
        }
        return -1;
    }
}
