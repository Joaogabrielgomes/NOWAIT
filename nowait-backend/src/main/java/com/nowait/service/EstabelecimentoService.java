package com.nowait.service;

import com.nowait.dto.EnderecoDTO;
import com.nowait.dto.EstabelecimentoRequestDTO;
import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.exception.DuplicateResourceException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.exception.UnauthorizedOperationException;
import com.nowait.model.Estabelecimento;
import com.nowait.model.Usuario;
import com.nowait.repository.EstabelecimentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EstabelecimentoService {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final CepService                cepService;

    public EstabelecimentoResponseDTO criar(EstabelecimentoRequestDTO dto, Usuario dono) {
        if (estabelecimentoRepository.existsByUsuarioId(dono.getId())) {
            throw new DuplicateResourceException("Estabelecimento", "usuário", dono.getId());
        }

        EnderecoDTO endereco = cepService.buscarEndereco(dto.getCep());

        Estabelecimento estabelecimento = Estabelecimento.builder()
                .usuario(dono)
                .nome(dto.getNome())
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

        EnderecoDTO endereco = cepService.buscarEndereco(dto.getCep());

        estabelecimento.setNome(dto.getNome());
        estabelecimento.setCep(endereco.getCep());
        estabelecimento.setLogradouro(endereco.getLogradouro());
        estabelecimento.setNumero(dto.getNumero());
        estabelecimento.setComplemento(dto.getComplemento());
        estabelecimento.setBairro(endereco.getBairro());
        estabelecimento.setCidade(endereco.getCidade());
        estabelecimento.setEstado(endereco.getEstado());

        estabelecimento = estabelecimentoRepository.save(estabelecimento);
        return toResponseDTO(estabelecimento);
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

    public List<EstabelecimentoResponseDTO> listarTodos() {
        return estabelecimentoRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
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
                .aberto(estabelecimento.getAberto())
                .createdAt(estabelecimento.getCreatedAt())
                .build();
    }
}
