package com.nowait.service;

import com.nowait.dto.FilaEsperaRequestDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.model.Estabelecimento;
import com.nowait.model.FilaEspera;
import com.nowait.model.Mesa;
import com.nowait.model.StatusAprovacaoEstabelecimento;
import com.nowait.model.StatusFila;
import com.nowait.model.StatusMesa;
import com.nowait.model.Usuario;
import com.nowait.repository.FilaEsperaRepository;
import com.nowait.repository.MesaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Testes da fila de espera: validações de entrada, chamada (com alocação de mesa) e
 * cancelamento (com liberação de mesa).
 */
@ExtendWith(MockitoExtension.class)
class FilaEsperaServiceTest {

    @Mock private FilaEsperaRepository filaEsperaRepository;
    @Mock private MesaRepository mesaRepository;
    @Mock private EstabelecimentoService estabelecimentoService;
    @Mock private AlocacaoMesaService alocacaoMesaService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private AuditoriaService auditoriaService;
    @Mock private EmailService emailService;

    @InjectMocks
    private FilaEsperaService filaEsperaService;

    private Estabelecimento estabelecimento;
    private Usuario cliente;
    private Usuario dono;

    @BeforeEach
    void setUp() {
        dono = Usuario.builder().id("dono-1").nome("Dono").email("dono@exemplo.com").build();
        estabelecimento = Estabelecimento.builder()
                .id("estab-1")
                .usuario(dono)
                .nome("Restaurante Teste")
                .aberto(true)
                .statusAprovacao(StatusAprovacaoEstabelecimento.APROVADO)
                .tempoMedioAtendimentoMinutos(15)
                .build();
        cliente = Usuario.builder().id("cliente-1").nome("Cliente").email("cliente@exemplo.com").build();

        // Stubs usados pela maioria dos cenários; lenient() porque nem todo teste
        // alcança o caminho que os consome (ex.: validações que lançam exceção antes).
        lenient().when(estabelecimentoService.buscarEntidadePorId("estab-1")).thenReturn(estabelecimento);
        lenient().when(filaEsperaRepository.save(any(FilaEspera.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(filaEsperaRepository
                .findByEstabelecimentoIdAndStatusOrderByHoraEntradaAsc("estab-1", StatusFila.CHAMADO))
                .thenReturn(List.of());
    }

    @Test
    void entrarNaFila_comEstabelecimentoNaoAprovado_lancaBusinessRuleException() {
        estabelecimento.setStatusAprovacao(StatusAprovacaoEstabelecimento.PENDENTE);

        FilaEsperaRequestDTO dto = new FilaEsperaRequestDTO();
        dto.setQuantidadePessoas(2);

        assertThatThrownBy(() -> filaEsperaService.entrarNaFila("estab-1", dto, cliente))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não está disponível");

        verify(filaEsperaRepository, never()).save(any());
    }

    @Test
    void entrarNaFila_semMesaComCapacidadeSuficiente_lancaBusinessRuleException() {
        when(mesaRepository.buscarMaiorCapacidade("estab-1")).thenReturn(4);

        FilaEsperaRequestDTO dto = new FilaEsperaRequestDTO();
        dto.setQuantidadePessoas(6);

        assertThatThrownBy(() -> filaEsperaService.entrarNaFila("estab-1", dto, cliente))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("capacidade");

        verify(filaEsperaRepository, never()).save(any());
    }

    @Test
    void entrarNaFila_comPosicaoAtivaExistente_lancaBusinessRuleException() {
        when(mesaRepository.buscarMaiorCapacidade("estab-1")).thenReturn(4);
        when(filaEsperaRepository.findByClienteIdAndEstabelecimentoIdAndStatusIn(
                eq("cliente-1"), eq("estab-1"), anyList()))
                .thenReturn(Optional.of(new FilaEspera()));

        FilaEsperaRequestDTO dto = new FilaEsperaRequestDTO();
        dto.setQuantidadePessoas(2);

        assertThatThrownBy(() -> filaEsperaService.entrarNaFila("estab-1", dto, cliente))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("posição ativa");

        verify(filaEsperaRepository, never()).save(any());
    }

    @Test
    void entrarNaFila_comDadosValidos_salvaEntradaComStatusAguardando() {
        when(mesaRepository.buscarMaiorCapacidade("estab-1")).thenReturn(4);
        when(filaEsperaRepository.findByClienteIdAndEstabelecimentoIdAndStatusIn(
                eq("cliente-1"), eq("estab-1"), anyList()))
                .thenReturn(Optional.empty());
        when(mesaRepository.countByEstabelecimentoId("estab-1")).thenReturn(10L);
        when(filaEsperaRepository.countByEstabelecimentoIdAndStatusIn(eq("estab-1"), anyList())).thenReturn(0L);
        when(filaEsperaRepository.findByEstabelecimentoIdAndStatusOrderByHoraEntradaAsc("estab-1", StatusFila.AGUARDANDO))
                .thenReturn(List.of());
        when(mesaRepository.findByEstabelecimentoIdAndStatusOrderByCapacidadeAsc("estab-1", StatusMesa.LIVRE))
                .thenReturn(List.of());

        FilaEsperaRequestDTO dto = new FilaEsperaRequestDTO();
        dto.setQuantidadePessoas(2);

        var resposta = filaEsperaService.entrarNaFila("estab-1", dto, cliente);

        assertThat(resposta.getStatus()).isEqualTo(StatusFila.AGUARDANDO);
        assertThat(resposta.getQuantidadePessoas()).isEqualTo(2);
        verify(filaEsperaRepository).save(any(FilaEspera.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/fila.estab-1"), any(Object.class));
    }

    @Test
    void chamar_comStatusDiferenteDeAguardando_lancaBusinessRuleException() {
        FilaEspera entrada = FilaEspera.builder()
                .id("fila-1").estabelecimento(estabelecimento).cliente(cliente)
                .quantidadePessoas(2).status(StatusFila.CHAMADO)
                .build();
        when(filaEsperaRepository.findById("fila-1")).thenReturn(Optional.of(entrada));

        assertThatThrownBy(() -> filaEsperaService.chamar("fila-1", dono))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("AGUARDANDO");

        verifyNoInteractions(alocacaoMesaService);
    }

    @Test
    void chamar_semMesaDisponivel_lancaBusinessRuleExceptionENaoAlteraStatus() {
        FilaEspera entrada = FilaEspera.builder()
                .id("fila-1").estabelecimento(estabelecimento).cliente(cliente)
                .quantidadePessoas(4).status(StatusFila.AGUARDANDO)
                .build();
        when(filaEsperaRepository.findById("fila-1")).thenReturn(Optional.of(entrada));
        when(alocacaoMesaService.alocarMesa("estab-1", 4)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> filaEsperaService.chamar("fila-1", dono))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Nenhuma mesa disponível");

        assertThat(entrada.getStatus()).isEqualTo(StatusFila.AGUARDANDO);
    }

    @Test
    void chamar_comMesaDisponivel_ocupaMesaEMudaStatusParaChamado() {
        FilaEspera entrada = FilaEspera.builder()
                .id("fila-1").estabelecimento(estabelecimento).cliente(cliente)
                .quantidadePessoas(2).status(StatusFila.AGUARDANDO)
                .build();
        Mesa mesa = Mesa.builder().id("mesa-1").numero("5").capacidade(4).status(StatusMesa.OCUPADA).build();

        when(filaEsperaRepository.findById("fila-1")).thenReturn(Optional.of(entrada));
        when(alocacaoMesaService.alocarMesa("estab-1", 2)).thenReturn(Optional.of(mesa));
        when(filaEsperaRepository.findByEstabelecimentoIdAndStatusOrderByHoraEntradaAsc("estab-1", StatusFila.AGUARDANDO))
                .thenReturn(List.of());
        when(mesaRepository.findByEstabelecimentoIdAndStatusOrderByCapacidadeAsc("estab-1", StatusMesa.LIVRE))
                .thenReturn(List.of());

        var resposta = filaEsperaService.chamar("fila-1", dono);

        assertThat(entrada.getStatus()).isEqualTo(StatusFila.CHAMADO);
        assertThat(entrada.getMesa()).isEqualTo(mesa);
        assertThat(resposta.getMesaId()).isEqualTo("mesa-1");
        // A mesa já chega OCUPADA de alocacaoMesaService — efetivarChamada não deve salvá-la de novo.
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void cancelar_comMesaAlocada_liberaMesaECancelaEntrada() {
        Mesa mesa = Mesa.builder().id("mesa-1").numero("5").capacidade(4).status(StatusMesa.OCUPADA).build();
        FilaEspera entrada = FilaEspera.builder()
                .id("fila-1").estabelecimento(estabelecimento).cliente(cliente).mesa(mesa)
                .quantidadePessoas(2).status(StatusFila.CHAMADO)
                .build();
        when(filaEsperaRepository.findById("fila-1")).thenReturn(Optional.of(entrada));
        when(filaEsperaRepository.findByEstabelecimentoIdAndStatusOrderByHoraEntradaAsc("estab-1", StatusFila.AGUARDANDO))
                .thenReturn(List.of());
        when(mesaRepository.findByEstabelecimentoIdAndStatusOrderByCapacidadeAsc("estab-1", StatusMesa.LIVRE))
                .thenReturn(List.of());

        filaEsperaService.cancelar("fila-1", cliente);

        assertThat(mesa.getStatus()).isEqualTo(StatusMesa.LIVRE);
        assertThat(entrada.getStatus()).isEqualTo(StatusFila.CANCELADO);
        verify(mesaRepository).save(mesa);
    }
}
