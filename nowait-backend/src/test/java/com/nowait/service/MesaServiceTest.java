package com.nowait.service;

import com.nowait.dto.MesaCapacidadeDTO;
import com.nowait.dto.MesaRequestDTO;
import com.nowait.dto.MesaResponseDTO;
import com.nowait.dto.MesaStatusDTO;
import com.nowait.exception.DuplicateResourceException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.Estabelecimento;
import com.nowait.model.Mesa;
import com.nowait.model.StatusMesa;
import com.nowait.model.Usuario;
import com.nowait.repository.MesaRepository;
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
 * Testes do MesaService: criação (com checagem de número duplicado), alteração de status
 * (com liberação de avaliação pendente de fila/agendamento quando a mesa é liberada), alteração
 * de capacidade e remoção — sempre validando que o solicitante é dono do estabelecimento.
 */
@ExtendWith(MockitoExtension.class)
class MesaServiceTest {

    @Mock private MesaRepository mesaRepository;
    @Mock private EstabelecimentoService estabelecimentoService;
    @Mock private FilaEsperaService filaEsperaService;
    @Mock private AgendamentoService agendamentoService;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private MesaService mesaService;

    private Usuario dono;
    private Estabelecimento estabelecimento;
    private Mesa mesa;

    @BeforeEach
    void setUp() {
        dono = Usuario.builder().id("usr-1").build();
        estabelecimento = Estabelecimento.builder().id("est-1").usuario(dono).build();
        mesa = Mesa.builder().id("mesa-1").estabelecimento(estabelecimento).numero("1")
                .capacidade(4).status(StatusMesa.LIVRE).build();
    }

    @Test
    void criar_lancaExcecaoQuandoNumeroJaExisteNoEstabelecimento() {
        MesaRequestDTO dto = new MesaRequestDTO();
        dto.setNumero("1");
        dto.setCapacidade(4);
        when(estabelecimentoService.buscarEntidadePorId("est-1")).thenReturn(estabelecimento);
        when(mesaRepository.existsByEstabelecimentoIdAndNumero("est-1", "1")).thenReturn(true);

        assertThatThrownBy(() -> mesaService.criar("est-1", dto, dono))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void criar_persisteMesaComStatusLivre() {
        MesaRequestDTO dto = new MesaRequestDTO();
        dto.setNumero("2");
        dto.setCapacidade(6);
        when(estabelecimentoService.buscarEntidadePorId("est-1")).thenReturn(estabelecimento);
        when(mesaRepository.existsByEstabelecimentoIdAndNumero("est-1", "2")).thenReturn(false);
        when(mesaRepository.save(any(Mesa.class))).thenAnswer(inv -> inv.getArgument(0));

        MesaResponseDTO resultado = mesaService.criar("est-1", dto, dono);

        assertThat(resultado.getStatus()).isEqualTo(StatusMesa.LIVRE);
        assertThat(resultado.getCapacidade()).isEqualTo(6);
        verify(auditoriaService).registrar(eq(dono), any(), eq("Mesa"), anyString(), anyString());
    }

    @Test
    void atualizarStatus_lancaExcecaoQuandoSolicitanteNaoEDono() {
        Usuario outro = Usuario.builder().id("usr-2").build();
        MesaStatusDTO dto = new MesaStatusDTO();
        dto.setStatus(StatusMesa.OCUPADA);
        when(mesaRepository.findById("mesa-1")).thenReturn(Optional.of(mesa));
        doThrow(new com.nowait.exception.UnauthorizedOperationException("sem permissão"))
                .when(estabelecimentoService).validarDono(estabelecimento, outro);

        assertThatThrownBy(() -> mesaService.atualizarStatus("mesa-1", dto, outro))
                .isInstanceOf(com.nowait.exception.UnauthorizedOperationException.class);
    }

    @Test
    void atualizarStatus_atualizaSemLiberarAvaliacaoQuandoNaoLivre() {
        MesaStatusDTO dto = new MesaStatusDTO();
        dto.setStatus(StatusMesa.OCUPADA);
        when(mesaRepository.findById("mesa-1")).thenReturn(Optional.of(mesa));
        when(mesaRepository.save(any(Mesa.class))).thenAnswer(inv -> inv.getArgument(0));

        mesaService.atualizarStatus("mesa-1", dto, dono);

        assertThat(mesa.getStatus()).isEqualTo(StatusMesa.OCUPADA);
        verify(filaEsperaService, never()).marcarAvaliacaoLiberadaPorMesa(anyString());
        verify(agendamentoService, never()).marcarAvaliacaoLiberadaPorMesa(anyString());
    }

    @Test
    void liberar_atualizaStatusELiberaAvaliacaoDeFilaEAgendamento() {
        mesa.setStatus(StatusMesa.OCUPADA);
        when(mesaRepository.findById("mesa-1")).thenReturn(Optional.of(mesa));
        when(mesaRepository.save(any(Mesa.class))).thenAnswer(inv -> inv.getArgument(0));

        mesaService.liberar("mesa-1", dono);

        assertThat(mesa.getStatus()).isEqualTo(StatusMesa.LIVRE);
        verify(filaEsperaService).marcarAvaliacaoLiberadaPorMesa("mesa-1");
        verify(agendamentoService).marcarAvaliacaoLiberadaPorMesa("mesa-1");
    }

    @Test
    void atualizarCapacidade_alteraCapacidadeDaMesa() {
        MesaCapacidadeDTO dto = new MesaCapacidadeDTO();
        dto.setCapacidade(8);
        when(mesaRepository.findById("mesa-1")).thenReturn(Optional.of(mesa));
        when(mesaRepository.save(any(Mesa.class))).thenAnswer(inv -> inv.getArgument(0));

        MesaResponseDTO resultado = mesaService.atualizarCapacidade("mesa-1", dto, dono);

        assertThat(resultado.getCapacidade()).isEqualTo(8);
    }

    @Test
    void remover_deletaMesaERegistraAuditoria() {
        when(mesaRepository.findById("mesa-1")).thenReturn(Optional.of(mesa));

        mesaService.remover("mesa-1", dono);

        verify(mesaRepository).delete(mesa);
        verify(auditoriaService).registrar(eq(dono), any(), eq("Mesa"), eq("mesa-1"), isNull());
    }

    @Test
    void buscarEntidadePorId_lancaExcecaoQuandoNaoEncontrada() {
        when(mesaRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mesaService.buscarEntidadePorId("x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
