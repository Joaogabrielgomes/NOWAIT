package com.nowait.service;

import com.nowait.dto.DenunciaCriacaoDTO;
import com.nowait.dto.DenunciaResolucaoDTO;
import com.nowait.dto.DenunciaResponseDTO;
import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.AcaoAuditoria;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DenunciaService {

    private static final String ENTIDADE_DENUNCIA = "Denuncia";

    private final DenunciaRepository        denunciaRepository;
    private final UsuarioRepository         usuarioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final AvaliacaoRepository       avaliacaoRepository;
    private final AuditoriaService          auditoriaService;

    public DenunciaResponseDTO criar(Usuario denunciante, DenunciaCriacaoDTO dto) {
        validarAlvoExiste(dto.getTipoAlvo(), dto.getAlvoId());
        validarNaoAutodenuncia(denunciante, dto.getTipoAlvo(), dto.getAlvoId());

        Denuncia denuncia = Denuncia.builder()
                .denunciante(denunciante)
                .tipoAlvo(dto.getTipoAlvo())
                .alvoId(dto.getAlvoId())
                .motivo(dto.getMotivo())
                .build();
        denuncia = denunciaRepository.save(denuncia);

        auditoriaService.registrar(denunciante, AcaoAuditoria.DENUNCIA_CRIADA, ENTIDADE_DENUNCIA, denuncia.getId(),
                "tipoAlvo=" + dto.getTipoAlvo());
        log.info("Denúncia criada: id={} denunciante={} tipoAlvo={} alvoId={}",
                denuncia.getId(), denunciante.getId(), dto.getTipoAlvo(), dto.getAlvoId());
        return toResponseDTO(denuncia);
    }

    public Page<DenunciaResponseDTO> listarPaginado(String status, String nome, int pagina, int tamanho) {
        PageRequest paginacao = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "createdAt"));
        String statusNormalizado = (status == null || status.isBlank()) ? null : status;
        String nomeNormalizado = (nome == null || nome.isBlank()) ? null : nome;
        Page<Denuncia> resultado = denunciaRepository.buscar(statusNormalizado, nomeNormalizado, paginacao);
        return resultado.map(this::toResponseDTO);
    }

    public DenunciaResponseDTO resolver(String id, Usuario admin, DenunciaResolucaoDTO dto) {
        Denuncia denuncia = buscarPorId(id);
        if (!StatusDenuncia.PENDENTE.equals(denuncia.getStatus())) {
            throw new BusinessRuleException("Esta denúncia já foi avaliada");
        }

        denuncia.setStatus(Boolean.TRUE.equals(dto.getAprovar()) ? StatusDenuncia.APROVADA : StatusDenuncia.REJEITADA);
        denuncia.setResolvidoPor(admin);
        denuncia.setObservacaoAdmin(dto.getObservacaoAdmin());
        denuncia.setResolvidoEm(LocalDateTime.now());

        if (Boolean.TRUE.equals(dto.getAprovar()) && TipoAlvoDenuncia.AVALIACAO.equals(denuncia.getTipoAlvo())) {
            avaliacaoRepository.findById(denuncia.getAlvoId()).ifPresent(avaliacaoRepository::delete);
        }

        denuncia = denunciaRepository.save(denuncia);
        log.info("Denúncia resolvida: id={} status={} admin={}", id, denuncia.getStatus(), admin.getId());
        return toResponseDTO(denuncia);
    }

    private Denuncia buscarPorId(String id) {
        return denunciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Denúncia", "id", id));
    }

    private void validarAlvoExiste(String tipoAlvo, String alvoId) {
        boolean existe = switch (tipoAlvo) {
            case TipoAlvoDenuncia.USUARIO -> usuarioRepository.existsById(alvoId);
            case TipoAlvoDenuncia.ESTABELECIMENTO -> estabelecimentoRepository.existsById(alvoId);
            case TipoAlvoDenuncia.AVALIACAO -> avaliacaoRepository.existsById(alvoId);
            default -> false;
        };
        if (!existe) {
            throw new ResourceNotFoundException("Alvo da denúncia", "id", alvoId);
        }
    }

    private void validarNaoAutodenuncia(Usuario denunciante, String tipoAlvo, String alvoId) {
        boolean autodenuncia = resolverUsuarioAlvoId(tipoAlvo, alvoId)
                .map(usuarioAlvoId -> usuarioAlvoId.equals(denunciante.getId()))
                .orElse(false);
        if (autodenuncia) {
            throw new BusinessRuleException("Não é possível denunciar a si mesmo ou seu próprio estabelecimento");
        }
    }

    private Optional<String> resolverUsuarioAlvoId(String tipoAlvo, String alvoId) {
        return switch (tipoAlvo) {
            case TipoAlvoDenuncia.USUARIO -> Optional.of(alvoId);
            case TipoAlvoDenuncia.ESTABELECIMENTO -> estabelecimentoRepository.findById(alvoId)
                    .map(Estabelecimento::getUsuario)
                    .map(Usuario::getId);
            case TipoAlvoDenuncia.AVALIACAO -> avaliacaoRepository.findById(alvoId)
                    .map(Avaliacao::getCliente)
                    .map(Usuario::getId);
            default -> Optional.empty();
        };
    }

    private String descreverAlvo(String tipoAlvo, String alvoId) {
        return switch (tipoAlvo) {
            case TipoAlvoDenuncia.USUARIO -> usuarioRepository.findById(alvoId)
                    .map(Usuario::getNome).orElse("Usuário removido");
            case TipoAlvoDenuncia.ESTABELECIMENTO -> estabelecimentoRepository.findById(alvoId)
                    .map(Estabelecimento::getNome).orElse("Estabelecimento removido");
            case TipoAlvoDenuncia.AVALIACAO -> avaliacaoRepository.findById(alvoId)
                    .map(a -> "Avaliação de " + a.getCliente().getNome()).orElse("Avaliação removida");
            default -> "Alvo desconhecido";
        };
    }

    private DenunciaResponseDTO toResponseDTO(Denuncia denuncia) {
        return DenunciaResponseDTO.builder()
                .id(denuncia.getId())
                .denuncianteId(denuncia.getDenunciante().getId())
                .denuncianteNome(denuncia.getDenunciante().getNome())
                .tipoAlvo(denuncia.getTipoAlvo())
                .alvoId(denuncia.getAlvoId())
                .alvoDescricao(descreverAlvo(denuncia.getTipoAlvo(), denuncia.getAlvoId()))
                .motivo(denuncia.getMotivo())
                .status(denuncia.getStatus())
                .resolvidoPorId(denuncia.getResolvidoPor() != null ? denuncia.getResolvidoPor().getId() : null)
                .resolvidoPorNome(denuncia.getResolvidoPor() != null ? denuncia.getResolvidoPor().getNome() : null)
                .observacaoAdmin(denuncia.getObservacaoAdmin())
                .usuarioAlvoId(resolverUsuarioAlvoId(denuncia.getTipoAlvo(), denuncia.getAlvoId()).orElse(null))
                .createdAt(denuncia.getCreatedAt())
                .resolvidoEm(denuncia.getResolvidoEm())
                .build();
    }
}
