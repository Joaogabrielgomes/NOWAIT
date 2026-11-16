package com.nowait.service;

import com.nowait.dto.EnderecoDTO;
import com.nowait.dto.EstabelecimentoRequestDTO;
import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.DuplicateResourceException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.exception.UnauthorizedOperationException;
import com.nowait.model.Estabelecimento;
import com.nowait.model.Perfil;
import com.nowait.model.StatusAprovacaoEstabelecimento;
import com.nowait.model.Usuario;
import com.nowait.repository.EstabelecimentoRepository;
import com.nowait.repository.MesaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes do EstabelecimentoService: cadastro (com integração ao ViaCEP e moderação de nome),
 * edição (incluindo o bloqueio de configurações personalizáveis antes da aprovação), abrir/fechar,
 * aprovação/rejeição pelo admin e validação de posse.
 */
@ExtendWith(MockitoExtension.class)
class EstabelecimentoServiceTest {

    @Mock private EstabelecimentoRepository estabelecimentoRepository;
    @Mock private MesaRepository mesaRepository;
    @Mock private CepService cepService;
    @Mock private ProcessadorImagemEstabelecimento processadorImagem;
    @Mock private ArmazenamentoArquivoService armazenamentoArquivoService;
    @Mock private ModeracaoService moderacaoService;
    @Mock private EmailService emailService;

    @InjectMocks
    private EstabelecimentoService estabelecimentoService;

    private Usuario dono;
    private Estabelecimento estabelecimento;
    private EnderecoDTO endereco;

    @BeforeEach
    void setUp() {
        dono = Usuario.builder().id("usr-1").nome("Dono").email("dono@example.com").role(Perfil.ESTABELECIMENTO).build();
        estabelecimento = Estabelecimento.builder()
                .id("est-1")
                .usuario(dono)
                .nome("Restaurante da Maria")
                .categoria("BRASILEIRA")
                .statusAprovacao(StatusAprovacaoEstabelecimento.PENDENTE)
                .horarioAbertura(LocalTime.of(8, 0))
                .horarioFechamento(LocalTime.of(22, 0))
                .build();
        endereco = EnderecoDTO.builder().cep("08773000").logradouro("Rua X").bairro("Centro")
                .cidade("Mogi das Cruzes").estado("SP").build();
    }

    private EstabelecimentoRequestDTO dtoBasico() {
        EstabelecimentoRequestDTO dto = new EstabelecimentoRequestDTO();
        dto.setNome("Restaurante da Maria");
        dto.setCategoria("BRASILEIRA");
        dto.setTelefone("11999999999");
        dto.setCep("08773-000");
        dto.setNumero("100");
        return dto;
    }

    @Test
    void criar_lancaExcecaoQuandoUsuarioJaPossuiEstabelecimento() {
        when(estabelecimentoRepository.existsByUsuarioId("usr-1")).thenReturn(true);

        assertThatThrownBy(() -> estabelecimentoService.criar(dtoBasico(), dono))
                .isInstanceOf(DuplicateResourceException.class);

        verify(cepService, never()).buscarEndereco(anyString());
    }

    @Test
    void criar_persisteComEnderecoResolvidoPeloCep() {
        when(estabelecimentoRepository.existsByUsuarioId("usr-1")).thenReturn(false);
        when(cepService.buscarEndereco("08773-000")).thenReturn(endereco);
        when(estabelecimentoRepository.save(any(Estabelecimento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mesaRepository.countByEstabelecimentoId(any())).thenReturn(0L);

        EstabelecimentoResponseDTO resultado = estabelecimentoService.criar(dtoBasico(), dono);

        assertThat(resultado.getNome()).isEqualTo("Restaurante da Maria");
        assertThat(resultado.getEndereco().getCidade()).isEqualTo("Mogi das Cruzes");
        verify(moderacaoService).verificar("Restaurante da Maria", "nome do estabelecimento");
    }

    @Test
    void atualizar_lancaExcecaoQuandoSolicitanteNaoEDono() {
        Usuario outroUsuario = Usuario.builder().id("usr-2").build();
        when(estabelecimentoRepository.findById("est-1")).thenReturn(Optional.of(estabelecimento));

        assertThatThrownBy(() -> estabelecimentoService.atualizar("est-1", dtoBasico(), outroUsuario))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void atualizar_ignoraConfiguracoesPersonalizaveisQuandoAindaNaoAprovado() {
        EstabelecimentoRequestDTO dto = dtoBasico();
        dto.setToleranciaPadraoMinutos(60);
        when(estabelecimentoRepository.findById("est-1")).thenReturn(Optional.of(estabelecimento));
        when(cepService.buscarEndereco(anyString())).thenReturn(endereco);
        when(estabelecimentoRepository.save(any(Estabelecimento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mesaRepository.countByEstabelecimentoId(any())).thenReturn(0L);

        estabelecimentoService.atualizar("est-1", dto, dono);

        // estabelecimento ainda está PENDENTE: a tolerância enviada deve ser ignorada
        assertThat(estabelecimento.getToleranciaPadraoMinutos()).isNotEqualTo(60);
    }

    @Test
    void atualizar_aplicaConfiguracoesPersonalizaveisQuandoJaAprovado() {
        estabelecimento.setStatusAprovacao(StatusAprovacaoEstabelecimento.APROVADO);
        EstabelecimentoRequestDTO dto = dtoBasico();
        dto.setToleranciaPadraoMinutos(45);
        when(estabelecimentoRepository.findById("est-1")).thenReturn(Optional.of(estabelecimento));
        when(cepService.buscarEndereco(anyString())).thenReturn(endereco);
        when(estabelecimentoRepository.save(any(Estabelecimento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mesaRepository.countByEstabelecimentoId(any())).thenReturn(0L);

        estabelecimentoService.atualizar("est-1", dto, dono);

        assertThat(estabelecimento.getToleranciaPadraoMinutos()).isEqualTo(45);
    }

    @Test
    void atualizar_lancaExcecaoQuandoHorarioFechamentoNaoEDepoisDaAbertura() {
        estabelecimento.setStatusAprovacao(StatusAprovacaoEstabelecimento.APROVADO);
        EstabelecimentoRequestDTO dto = dtoBasico();
        dto.setHorarioAbertura(LocalTime.of(22, 0));
        dto.setHorarioFechamento(LocalTime.of(8, 0));
        when(estabelecimentoRepository.findById("est-1")).thenReturn(Optional.of(estabelecimento));
        when(cepService.buscarEndereco(anyString())).thenReturn(endereco);

        assertThatThrownBy(() -> estabelecimentoService.atualizar("est-1", dto, dono))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void alternarAberto_lancaExcecaoQuandoNaoAprovado() {
        when(estabelecimentoRepository.findById("est-1")).thenReturn(Optional.of(estabelecimento));

        assertThatThrownBy(() -> estabelecimentoService.alternarAberto("est-1", dono))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void alternarAberto_inverteStatusQuandoAprovado() {
        estabelecimento.setStatusAprovacao(StatusAprovacaoEstabelecimento.APROVADO);
        estabelecimento.setAberto(true);
        when(estabelecimentoRepository.findById("est-1")).thenReturn(Optional.of(estabelecimento));
        when(estabelecimentoRepository.save(any(Estabelecimento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mesaRepository.countByEstabelecimentoId(any())).thenReturn(0L);

        estabelecimentoService.alternarAberto("est-1", dono);

        assertThat(estabelecimento.getAberto()).isFalse();
    }

    @Test
    void aprovar_alteraStatusParaAprovado() {
        when(estabelecimentoRepository.findById("est-1")).thenReturn(Optional.of(estabelecimento));
        when(estabelecimentoRepository.save(any(Estabelecimento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mesaRepository.countByEstabelecimentoId(any())).thenReturn(0L);

        EstabelecimentoResponseDTO resultado = estabelecimentoService.aprovar("est-1");

        assertThat(resultado.getStatusAprovacao()).isEqualTo(StatusAprovacaoEstabelecimento.APROVADO);
    }

    @Test
    void rejeitar_alteraStatusParaRejeitado() {
        when(estabelecimentoRepository.findById("est-1")).thenReturn(Optional.of(estabelecimento));
        when(estabelecimentoRepository.save(any(Estabelecimento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mesaRepository.countByEstabelecimentoId(any())).thenReturn(0L);

        EstabelecimentoResponseDTO resultado = estabelecimentoService.rejeitar("est-1", "Dados incompletos");

        assertThat(resultado.getStatusAprovacao()).isEqualTo(StatusAprovacaoEstabelecimento.REJEITADO);
    }

    @Test
    void buscarEntidadePorId_lancaExcecaoQuandoNaoEncontrado() {
        when(estabelecimentoRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estabelecimentoService.buscarEntidadePorId("x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buscarPorDono_lancaExcecaoQuandoUsuarioNaoTemEstabelecimento() {
        when(estabelecimentoRepository.findByUsuarioId("usr-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estabelecimentoService.buscarPorDono(dono))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void validarDono_lancaExcecaoQuandoSolicitanteNaoEOUsuarioDoEstabelecimento() {
        Usuario outro = Usuario.builder().id("usr-2").build();

        assertThatThrownBy(() -> estabelecimentoService.validarDono(estabelecimento, outro))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void validarDono_naoLancaExcecaoQuandoSolicitanteEDono() {
        estabelecimentoService.validarDono(estabelecimento, dono);
        // nenhuma exceção deve ser lançada quando o solicitante é o dono
    }
}
