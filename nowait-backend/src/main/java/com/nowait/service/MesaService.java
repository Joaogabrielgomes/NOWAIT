package com.nowait.service;

import com.nowait.dto.MesaRequestDTO;
import com.nowait.dto.MesaResponseDTO;
import com.nowait.dto.MesaStatusDTO;
import com.nowait.exception.DuplicateResourceException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.Estabelecimento;
import com.nowait.model.Mesa;
import com.nowait.model.StatusMesa;
import com.nowait.model.Usuario;
import com.nowait.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MesaService {

    private static final String ENTIDADE_MESA = "Mesa";

    private final MesaRepository         mesaRepository;
    private final EstabelecimentoService estabelecimentoService;
    private final FilaEsperaService      filaEsperaService;
    private final AuditoriaService       auditoriaService;

    public MesaResponseDTO criar(String estabelecimentoId, MesaRequestDTO dto, Usuario solicitante) {
        Estabelecimento estabelecimento = estabelecimentoService.buscarEntidadePorId(estabelecimentoId);
        estabelecimentoService.validarDono(estabelecimento, solicitante);

        if (mesaRepository.existsByEstabelecimentoIdAndNumero(estabelecimentoId, dto.getNumero())) {
            throw new DuplicateResourceException("Mesa", "número", dto.getNumero());
        }

        Mesa mesa = Mesa.builder()
                .estabelecimento(estabelecimento)
                .numero(dto.getNumero())
                .capacidade(dto.getCapacidade())
                .status(StatusMesa.LIVRE)
                .build();

        mesa = mesaRepository.save(mesa);
        log.info("Mesa criada: id={} estabelecimento={}", mesa.getId(), estabelecimentoId);

        auditoriaService.registrar(solicitante, AcaoAuditoria.MESA_CRIADA, ENTIDADE_MESA, mesa.getId(),
                "numero=" + mesa.getNumero() + ", capacidade=" + mesa.getCapacidade());
        return toResponseDTO(mesa);
    }

    public List<MesaResponseDTO> listarPorEstabelecimento(String estabelecimentoId) {
        return mesaRepository.findByEstabelecimentoIdOrderByNumeroAsc(estabelecimentoId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public MesaResponseDTO atualizarStatus(String mesaId, MesaStatusDTO dto, Usuario solicitante) {
        Mesa mesa = buscarEntidadePorId(mesaId);
        estabelecimentoService.validarDono(mesa.getEstabelecimento(), solicitante);
        return aplicarNovoStatus(mesa, dto.getStatus(), solicitante);
    }

    public MesaResponseDTO liberar(String mesaId, Usuario solicitante) {
        Mesa mesa = buscarEntidadePorId(mesaId);
        estabelecimentoService.validarDono(mesa.getEstabelecimento(), solicitante);
        return aplicarNovoStatus(mesa, StatusMesa.LIVRE, solicitante);
    }

    private MesaResponseDTO aplicarNovoStatus(Mesa mesa, String novoStatus, Usuario solicitante) {
        mesa.setStatus(novoStatus);
        mesa = mesaRepository.save(mesa);

        boolean liberada = StatusMesa.LIVRE.equals(novoStatus);
        auditoriaService.registrar(solicitante,
                liberada ? AcaoAuditoria.MESA_LIBERADA : AcaoAuditoria.MESA_STATUS_ALTERADO,
                ENTIDADE_MESA, mesa.getId(), "status=" + novoStatus);

        if (liberada) {
            filaEsperaService.tentarChamarProximoAutomaticamente(mesa.getEstabelecimento().getId());
        }

        return toResponseDTO(mesa);
    }

    public void remover(String mesaId, Usuario solicitante) {
        Mesa mesa = buscarEntidadePorId(mesaId);
        estabelecimentoService.validarDono(mesa.getEstabelecimento(), solicitante);
        auditoriaService.registrar(solicitante, AcaoAuditoria.MESA_REMOVIDA, ENTIDADE_MESA, mesa.getId(), null);
        mesaRepository.delete(mesa);
    }

    public Mesa buscarEntidadePorId(String id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa", "id", id));
    }

    private MesaResponseDTO toResponseDTO(Mesa mesa) {
        return MesaResponseDTO.builder()
                .id(mesa.getId())
                .estabelecimentoId(mesa.getEstabelecimento().getId())
                .numero(mesa.getNumero())
                .capacidade(mesa.getCapacidade())
                .status(mesa.getStatus())
                .build();
    }
}
