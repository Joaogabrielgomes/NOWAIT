package com.nowait.service;

import com.nowait.dto.DenunciaCriacaoDTO;
import com.nowait.dto.DenunciaResolucaoDTO;
import com.nowait.dto.DenunciaResponseDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.Avaliacao;
import com.nowait.model.Denuncia;
import com.nowait.model.Estabelecimento;
import com.nowait.model.StatusDenuncia;
import com.nowait.model.TipoAlvoDenuncia;
import com.nowait.model.Usuario;
import com.nowait.repository.AvaliacaoRepository;
import com.nowait.repository.DenunciaRepository;
import com.nowait.repository.EstabelecimentoRepository;
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
 * Testes do DenunciaService: criação (validando que o alvo existe e bloqueando autodenúncia),
 * listagem e resolução (aprovação remove a avaliação denunciada quando o alvo é uma avaliação;
 * denúncia já resolvida não pode ser resolvida de novo).
 */
@ExtendWith(MockitoExtension.class)
class DenunciaServiceTest {

    @Mock private DenunciaRepository denunciaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstabelecimentoRepository estabelecimentoRepository;
    @Mock private AvaliacaoRepository avaliacaoRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private DenunciaService denunciaService;

    private Usuario denunciante;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        denunciante = Usuario.builder().id("usr-1").nome("Denunciante").build();
        admin = Usuario.builder().id("admin-1").nome("Admin").build();
    }

    @Test
    void criar_lancaExcecaoQuandoAlvoNaoExiste() {
        DenunciaCriacaoDTO dto = new DenunciaCriacaoDTO();
        dto.setTipoAlvo(TipoAlvoDenuncia.USUARIO);
        dto.setAlvoId("usr-alvo");
        dto.setMotivo("comportamento inadequado");
        when(usuarioRepository.existsById("usr-alvo")).thenReturn(false);

        assertThatThrownBy(() -> denunciaService.criar(denunciante, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void criar_lancaExcecaoQuandoEhAutodenuncia() {
        DenunciaCriacaoDTO dto = new DenunciaCriacaoDTO();
        dto.setTipoAlvo(TipoAlvoDenuncia.USUARIO);
        dto.setAlvoId("usr-1");
        dto.setMotivo("teste");
        when(usuarioRepository.existsById("usr-1")).thenReturn(true);

        assertThatThrownBy(() -> denunciaService.criar(denunciante, dto))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void criar_persisteDenunciaContraOutroUsuario() {
        DenunciaCriacaoDTO dto = new DenunciaCriacaoDTO();
        dto.setTipoAlvo(TipoAlvoDenuncia.USUARIO);
        dto.setAlvoId("usr-alvo");
        dto.setMotivo("spam");
        when(usuarioRepository.existsById("usr-alvo")).thenReturn(true);
        Usuario alvo = Usuario.builder().id("usr-alvo").nome("Alvo").build();
        when(usuarioRepository.findById("usr-alvo")).thenReturn(Optional.of(alvo));
        when(denunciaRepository.save(any(Denuncia.class))).thenAnswer(inv -> {
            Denuncia d = inv.getArgument(0);
            d.setId("denuncia-1");
            return d;
        });

        DenunciaResponseDTO resultado = denunciaService.criar(denunciante, dto);

        assertThat(resultado.getStatus()).isEqualTo(StatusDenuncia.PENDENTE);
        assertThat(resultado.getUsuarioAlvoId()).isEqualTo("usr-alvo");
        verify(auditoriaService).registrar(eq(denunciante), any(), eq("Denuncia"), anyString(), anyString());
    }

    @Test
    void resolver_lancaExcecaoQuandoDenunciaJaFoiAvaliada() {
        Denuncia denuncia = Denuncia.builder().id("d-1").denunciante(denunciante)
                .tipoAlvo(TipoAlvoDenuncia.USUARIO).alvoId("usr-alvo")
                .status(StatusDenuncia.APROVADA).build();
        when(denunciaRepository.findById("d-1")).thenReturn(Optional.of(denuncia));

        DenunciaResolucaoDTO dto = new DenunciaResolucaoDTO();
        dto.setAprovar(true);

        assertThatThrownBy(() -> denunciaService.resolver("d-1", admin, dto))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resolver_aprovandoDenunciaDeAvaliacaoRemoveAAvaliacao() {
        Denuncia denuncia = Denuncia.builder().id("d-1").denunciante(denunciante)
                .tipoAlvo(TipoAlvoDenuncia.AVALIACAO).alvoId("aval-1")
                .status(StatusDenuncia.PENDENTE).build();
        when(denunciaRepository.findById("d-1")).thenReturn(Optional.of(denuncia));
        when(denunciaRepository.save(any(Denuncia.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario autorAvaliacao = Usuario.builder().id("usr-autor").nome("Autor").build();
        Avaliacao avaliacao = Avaliacao.builder().id("aval-1").cliente(autorAvaliacao).build();
        when(avaliacaoRepository.findById("aval-1")).thenReturn(Optional.of(avaliacao));

        DenunciaResolucaoDTO dto = new DenunciaResolucaoDTO();
        dto.setAprovar(true);
        dto.setObservacaoAdmin("Conteúdo ofensivo");

        DenunciaResponseDTO resultado = denunciaService.resolver("d-1", admin, dto);

        assertThat(resultado.getStatus()).isEqualTo(StatusDenuncia.APROVADA);
        verify(avaliacaoRepository).delete(avaliacao);
    }

    @Test
    void resolver_rejeitandoDenunciaNaoRemoveNada() {
        Denuncia denuncia = Denuncia.builder().id("d-1").denunciante(denunciante)
                .tipoAlvo(TipoAlvoDenuncia.AVALIACAO).alvoId("aval-1")
                .status(StatusDenuncia.PENDENTE).build();
        when(denunciaRepository.findById("d-1")).thenReturn(Optional.of(denuncia));
        when(denunciaRepository.save(any(Denuncia.class))).thenAnswer(inv -> inv.getArgument(0));

        DenunciaResolucaoDTO dto = new DenunciaResolucaoDTO();
        dto.setAprovar(false);

        DenunciaResponseDTO resultado = denunciaService.resolver("d-1", admin, dto);

        assertThat(resultado.getStatus()).isEqualTo(StatusDenuncia.REJEITADA);
        verify(avaliacaoRepository, never()).delete(any());
    }
}
