package com.nowait.service;

import com.nowait.dto.EnderecoDTO;
import com.nowait.dto.EstabelecimentoRequestDTO;
import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.DuplicateResourceException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.exception.UnauthorizedOperationException;
import com.nowait.model.Estabelecimento;
import com.nowait.model.StatusAprovacaoEstabelecimento;
import com.nowait.model.StatusMesa;
import com.nowait.model.Usuario;
import com.nowait.repository.EstabelecimentoRepository;
import com.nowait.repository.EstabelecimentoSpecifications;
import com.nowait.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EstabelecimentoService {

    private static final int LOGO_LARGURA_MINIMA = 300;
    private static final int LOGO_ALTURA_MINIMA = 300;
    private static final int LOGO_LARGURA_MAXIMA = 800;

    private static final int BANNER_LARGURA_MINIMA = 1000;
    private static final int BANNER_ALTURA_MINIMA = 250;
    private static final int BANNER_LARGURA_MAXIMA = 1600;

    private static final String SUBPASTA_UPLOAD = "estabelecimentos";

    private final EstabelecimentoRepository      estabelecimentoRepository;
    private final MesaRepository                 mesaRepository;
    private final CepService                     cepService;
    private final ProcessadorImagemEstabelecimento processadorImagem;
    private final ArmazenamentoArquivoService    armazenamentoArquivoService;
    private final ModeracaoService               moderacaoService;
    private final EmailService                   emailService;

    public EstabelecimentoResponseDTO criar(EstabelecimentoRequestDTO dto, Usuario dono) {
        if (estabelecimentoRepository.existsByUsuarioId(dono.getId())) {
            throw new DuplicateResourceException("Estabelecimento", "usuário", dono.getId());
        }
        moderacaoService.verificar(dto.getNome(), "nome do estabelecimento");

        EnderecoDTO endereco = cepService.buscarEndereco(dto.getCep());

        Estabelecimento estabelecimento = Estabelecimento.builder()
                .usuario(dono)
                .nome(dto.getNome())
                .categoria(dto.getCategoria())
                .telefone(dto.getTelefone())
                .cep(endereco.getCep())
                .logradouro(endereco.getLogradouro())
                .numero(dto.getNumero())
                .complemento(dto.getComplemento())
                .bairro(endereco.getBairro())
                .cidade(endereco.getCidade())
                .estado(endereco.getEstado())
                .build();

        estabelecimento = estabelecimentoRepository.save(estabelecimento);
        log.info("Estabelecimento criado: id={} dono={}", estabelecimento.getId(), dono.getId());
        return toResponseDTO(estabelecimento);
    }

    public EstabelecimentoResponseDTO atualizar(String id, EstabelecimentoRequestDTO dto, Usuario solicitante) {
        Estabelecimento estabelecimento = buscarEntidadePorId(id);
        validarDono(estabelecimento, solicitante);
        moderacaoService.verificar(dto.getNome(), "nome do estabelecimento");

        EnderecoDTO endereco = cepService.buscarEndereco(dto.getCep());

        estabelecimento.setNome(dto.getNome());
        estabelecimento.setCategoria(dto.getCategoria());
        estabelecimento.setTelefone(dto.getTelefone());
        estabelecimento.setCep(endereco.getCep());
        estabelecimento.setLogradouro(endereco.getLogradouro());
        estabelecimento.setNumero(dto.getNumero());
        estabelecimento.setComplemento(dto.getComplemento());
        estabelecimento.setBairro(endereco.getBairro());
        estabelecimento.setCidade(endereco.getCidade());
        estabelecimento.setEstado(endereco.getEstado());

        aplicarConfiguracoesPersonalizaveis(estabelecimento, dto);

        estabelecimento = estabelecimentoRepository.save(estabelecimento);
        return toResponseDTO(estabelecimento);
    }

    private void aplicarConfiguracoesPersonalizaveis(Estabelecimento estabelecimento, EstabelecimentoRequestDTO dto) {
        if (!StatusAprovacaoEstabelecimento.APROVADO.equals(estabelecimento.getStatusAprovacao())) {
            return;
        }

        if (dto.getToleranciaPadraoMinutos() != null) {
            estabelecimento.setToleranciaPadraoMinutos(dto.getToleranciaPadraoMinutos());
        }
        if (dto.getTempoMedioAtendimentoMinutos() != null) {
            estabelecimento.setTempoMedioAtendimentoMinutos(dto.getTempoMedioAtendimentoMinutos());
        }
        if (dto.getAntecedenciaMaximaDias() != null) {
            estabelecimento.setAntecedenciaMaximaDias(dto.getAntecedenciaMaximaDias());
        }
        if (dto.getAntecedenciaReservaMesaMinutos() != null) {
            estabelecimento.setAntecedenciaReservaMesaMinutos(dto.getAntecedenciaReservaMesaMinutos());
        }
        if (dto.getAntecedenciaMinimaMinutos() != null) {
            estabelecimento.setAntecedenciaMinimaMinutos(dto.getAntecedenciaMinimaMinutos());
        }
        if (dto.getHorarioAbertura() != null) {
            estabelecimento.setHorarioAbertura(dto.getHorarioAbertura());
        }
        if (dto.getHorarioFechamento() != null) {
            estabelecimento.setHorarioFechamento(dto.getHorarioFechamento());
        }

        if (!estabelecimento.getHorarioFechamento().isAfter(estabelecimento.getHorarioAbertura())) {
            throw new BusinessRuleException("O horário de fechamento deve ser depois do horário de abertura");
        }
    }

    public EstabelecimentoResponseDTO alternarAberto(String id, Usuario solicitante) {
        Estabelecimento estabelecimento = buscarEntidadePorId(id);
        validarDono(estabelecimento, solicitante);

        if (!StatusAprovacaoEstabelecimento.APROVADO.equals(estabelecimento.getStatusAprovacao())) {
            throw new BusinessRuleException("Seu estabelecimento ainda não foi aprovado pelo administrador");
        }

        estabelecimento.setAberto(!Boolean.TRUE.equals(estabelecimento.getAberto()));
        estabelecimento = estabelecimentoRepository.save(estabelecimento);
        return toResponseDTO(estabelecimento);
    }

    public EstabelecimentoResponseDTO aprovar(String id) {
        Estabelecimento estabelecimento = buscarEntidadePorId(id);
        estabelecimento.setStatusAprovacao(StatusAprovacaoEstabelecimento.APROVADO);
        estabelecimento.setMotivoRejeicao(null);
        estabelecimento.setTokenContestacaoRejeicao(null);
        estabelecimento = estabelecimentoRepository.save(estabelecimento);
        log.info("Estabelecimento aprovado: id={}", id);
        return toResponseDTO(estabelecimento);
    }

    public EstabelecimentoResponseDTO rejeitar(String id, String motivo) {
        Estabelecimento estabelecimento = buscarEntidadePorId(id);
        estabelecimento.setStatusAprovacao(StatusAprovacaoEstabelecimento.REJEITADO);
        estabelecimento.setMotivoRejeicao(motivo);
        estabelecimento.setTokenContestacaoRejeicao(UUID.randomUUID().toString());
        estabelecimento = estabelecimentoRepository.save(estabelecimento);
        log.info("Estabelecimento rejeitado: id={}", id);

        Estabelecimento rejeitado = estabelecimento;
        emailService.enviarComSeguranca(() -> emailService.enviarEmailRejeicaoEstabelecimento(
                rejeitado.getUsuario().getEmail(), rejeitado.getNome(), motivo, rejeitado.getTokenContestacaoRejeicao()));

        return toResponseDTO(estabelecimento);
    }

    public EstabelecimentoResponseDTO atualizarLogo(String id, MultipartFile arquivo, Usuario solicitante) {
        Estabelecimento estabelecimento = buscarEntidadePorId(id);
        validarDono(estabelecimento, solicitante);

        byte[] imagemProcessada =
                processadorImagem.processar(arquivo, LOGO_LARGURA_MINIMA, LOGO_ALTURA_MINIMA, LOGO_LARGURA_MAXIMA);
        String caminho = armazenamentoArquivoService.salvar(SUBPASTA_UPLOAD, id + "-logo.jpg", imagemProcessada);

        estabelecimento.setLogoUrl(caminho);
        estabelecimento = estabelecimentoRepository.save(estabelecimento);
        return toResponseDTO(estabelecimento);
    }

    public EstabelecimentoResponseDTO atualizarBanner(String id, MultipartFile arquivo, Usuario solicitante) {
        Estabelecimento estabelecimento = buscarEntidadePorId(id);
        validarDono(estabelecimento, solicitante);

        byte[] imagemProcessada =
                processadorImagem.processar(arquivo, BANNER_LARGURA_MINIMA, BANNER_ALTURA_MINIMA, BANNER_LARGURA_MAXIMA);
        String caminho = armazenamentoArquivoService.salvar(SUBPASTA_UPLOAD, id + "-banner.jpg", imagemProcessada);

        estabelecimento.setBannerUrl(caminho);
        estabelecimento = estabelecimentoRepository.save(estabelecimento);
        return toResponseDTO(estabelecimento);
    }

    public EstabelecimentoResponseDTO removerLogo(String id, Usuario solicitante) {
        Estabelecimento estabelecimento = buscarEntidadePorId(id);
        validarDono(estabelecimento, solicitante);

        armazenamentoArquivoService.remover(SUBPASTA_UPLOAD, id + "-logo.jpg");
        estabelecimento.setLogoUrl(null);
        estabelecimento = estabelecimentoRepository.save(estabelecimento);
        return toResponseDTO(estabelecimento);
    }

    public EstabelecimentoResponseDTO removerBanner(String id, Usuario solicitante) {
        Estabelecimento estabelecimento = buscarEntidadePorId(id);
        validarDono(estabelecimento, solicitante);

        armazenamentoArquivoService.remover(SUBPASTA_UPLOAD, id + "-banner.jpg");
        estabelecimento.setBannerUrl(null);
        estabelecimento = estabelecimentoRepository.save(estabelecimento);
        return toResponseDTO(estabelecimento);
    }

    public void removerImagensDoEstabelecimento(Estabelecimento estabelecimento) {
        armazenamentoArquivoService.remover(SUBPASTA_UPLOAD, estabelecimento.getId() + "-logo.jpg");
        armazenamentoArquivoService.remover(SUBPASTA_UPLOAD, estabelecimento.getId() + "-banner.jpg");

        estabelecimento.setLogoUrl(null);
        estabelecimento.setBannerUrl(null);
        estabelecimentoRepository.save(estabelecimento);
    }

    public EstabelecimentoResponseDTO buscarPorId(String id) {
        return toResponseDTO(buscarEntidadePorId(id));
    }

    public EstabelecimentoResponseDTO buscarPorDono(Usuario dono) {
        Estabelecimento estabelecimento = estabelecimentoRepository.findByUsuarioId(dono.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Este usuário ainda não cadastrou um estabelecimento"));
        return toResponseDTO(estabelecimento);
    }

    public Page<EstabelecimentoResponseDTO> listarPaginado(
            String statusAprovacao, String nome, String campoOrdenacao, Sort.Direction direcao, int pagina, int tamanho) {
        PageRequest paginacao = PageRequest.of(pagina, tamanho, Sort.by(direcao, campoOrdenacao));
        Specification<Estabelecimento> filtro = Specification
                .where(EstabelecimentoSpecifications.comStatusAprovacao(statusAprovacao))
                .and(EstabelecimentoSpecifications.comNome(nome));
        return estabelecimentoRepository.findAll(filtro, paginacao).map(this::toResponseDTO);
    }

    public Page<EstabelecimentoResponseDTO> listarPublico(
            String nome, String cidade, String bairro, String categoria, int pagina, int tamanho) {
        PageRequest paginacao = PageRequest.of(pagina, tamanho, Sort.by("nome").ascending());

        Specification<Estabelecimento> filtro = Specification
                .where(EstabelecimentoSpecifications.aprovadoEAberto(StatusAprovacaoEstabelecimento.APROVADO))
                .and(EstabelecimentoSpecifications.comNome(nome))
                .and(EstabelecimentoSpecifications.comCidade(cidade))
                .and(EstabelecimentoSpecifications.comBairro(bairro))
                .and(EstabelecimentoSpecifications.comCategoria(categoria));

        return estabelecimentoRepository.findAll(filtro, paginacao).map(this::toResponseDTO);
    }

    public Estabelecimento buscarEntidadePorId(String id) {
        return estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento", "id", id));
    }

    public void validarDono(Estabelecimento estabelecimento, Usuario solicitante) {
        if (!estabelecimento.getUsuario().getId().equals(solicitante.getId())) {
            throw new UnauthorizedOperationException(
                    "Você não tem permissão para gerenciar este estabelecimento");
        }
    }

    private EstabelecimentoResponseDTO toResponseDTO(Estabelecimento estabelecimento) {
        return EstabelecimentoResponseDTO.builder()
                .id(estabelecimento.getId())
                .usuarioId(estabelecimento.getUsuario().getId())
                .nome(estabelecimento.getNome())
                .categoria(estabelecimento.getCategoria())
                .telefone(estabelecimento.getTelefone())
                .endereco(EnderecoDTO.builder()
                        .cep(estabelecimento.getCep())
                        .logradouro(estabelecimento.getLogradouro())
                        .numero(estabelecimento.getNumero())
                        .complemento(estabelecimento.getComplemento())
                        .bairro(estabelecimento.getBairro())
                        .cidade(estabelecimento.getCidade())
                        .estado(estabelecimento.getEstado())
                        .build())
                .tempoMedioAtendimentoMinutos(estabelecimento.getTempoMedioAtendimentoMinutos())
                .toleranciaPadraoMinutos(estabelecimento.getToleranciaPadraoMinutos())
                .horarioAbertura(estabelecimento.getHorarioAbertura())
                .horarioFechamento(estabelecimento.getHorarioFechamento())
                .antecedenciaMaximaDias(estabelecimento.getAntecedenciaMaximaDias())
                .antecedenciaReservaMesaMinutos(estabelecimento.getAntecedenciaReservaMesaMinutos())
                .antecedenciaMinimaMinutos(estabelecimento.getAntecedenciaMinimaMinutos())
                .aberto(estabelecimento.getAberto())
                .statusAprovacao(estabelecimento.getStatusAprovacao())
                .totalMesas((int) mesaRepository.countByEstabelecimentoId(estabelecimento.getId()))
                .mesasDisponiveis((int) mesaRepository.countByEstabelecimentoIdAndStatus(estabelecimento.getId(), StatusMesa.LIVRE))
                .logoUrl(estabelecimento.getLogoUrl())
                .bannerUrl(estabelecimento.getBannerUrl())
                .createdAt(estabelecimento.getCreatedAt())
                .build();
    }
}
