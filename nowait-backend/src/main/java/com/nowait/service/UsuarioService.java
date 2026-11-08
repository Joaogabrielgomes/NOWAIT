package com.nowait.service;

import com.nowait.dto.EnderecoDTO;
import com.nowait.dto.UsuarioAtualizacaoDTO;
import com.nowait.dto.UsuarioResponseDTO;
import com.nowait.exception.DuplicateResourceException;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.Usuario;
import com.nowait.repository.AgendamentoRepository;
import com.nowait.repository.EstabelecimentoRepository;
import com.nowait.repository.FilaEsperaRepository;
import com.nowait.repository.PasswordResetTokenRepository;
import com.nowait.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

    private static final String ENTIDADE_USUARIO = "Usuario";
    private static final String NOME_ANONIMIZADO = "Usuário removido";

    private final UsuarioRepository            usuarioRepository;
    private final EstabelecimentoRepository     estabelecimentoRepository;
    private final FilaEsperaRepository          filaEsperaRepository;
    private final AgendamentoRepository         agendamentoRepository;
    private final PasswordResetTokenRepository  passwordResetTokenRepository;
    private final PasswordEncoder               passwordEncoder;
    private final AuditoriaService               auditoriaService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }

    public Usuario buscarPorId(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", id));
    }

    public UsuarioResponseDTO atualizarPerfil(Usuario usuario, UsuarioAtualizacaoDTO dto) {
        if (usuarioRepository.existsByEmailAndIdNot(dto.getEmail(), usuario.getId())) {
            throw new DuplicateResourceException("Usuário", "e-mail", dto.getEmail());
        }

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
        }

        usuario = usuarioRepository.save(usuario);
        auditoriaService.registrar(usuario, AcaoAuditoria.USUARIO_PERFIL_ATUALIZADO, ENTIDADE_USUARIO,
                usuario.getId(), null);
        log.info("Perfil atualizado: usuarioId={}", usuario.getId());
        return toResponseDTO(usuario);
    }

    public void excluirPropriaConta(Usuario usuario) {
        String usuarioId = usuario.getId();
        boolean possuiHistoricoVinculado = estabelecimentoRepository.existsByUsuarioId(usuarioId)
                || filaEsperaRepository.existsByClienteId(usuarioId)
                || agendamentoRepository.existsByClienteId(usuarioId);

        passwordResetTokenRepository.deleteByUsuarioId(usuarioId);

        if (possuiHistoricoVinculado) {
            anonimizarDadosPessoais(usuario);
            auditoriaService.registrar(usuarioId, AcaoAuditoria.USUARIO_CONTA_ANONIMIZADA, ENTIDADE_USUARIO,
                    usuarioId, null);
            log.info("Conta anonimizada: usuarioId={}", usuarioId);
        } else {
            usuarioRepository.delete(usuario);
            auditoriaService.registrar(usuarioId, AcaoAuditoria.USUARIO_CONTA_EXCLUIDA, ENTIDADE_USUARIO,
                    usuarioId, null);
            log.info("Conta excluída: usuarioId={}", usuarioId);
        }
    }

    private void anonimizarDadosPessoais(Usuario usuario) {
        usuario.setNome(NOME_ANONIMIZADO);
        usuario.setEmail("removido-" + usuario.getId() + "@anonimizado.nowait");
        usuario.setSenhaHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setCep(null);
        usuario.setLogradouro(null);
        usuario.setNumero(null);
        usuario.setComplemento(null);
        usuario.setBairro(null);
        usuario.setCidade(null);
        usuario.setEstado(null);
        usuarioRepository.save(usuario);
    }

    public UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .endereco(EnderecoDTO.builder()
                        .cep(usuario.getCep())
                        .logradouro(usuario.getLogradouro())
                        .numero(usuario.getNumero())
                        .complemento(usuario.getComplemento())
                        .bairro(usuario.getBairro())
                        .cidade(usuario.getCidade())
                        .estado(usuario.getEstado())
                        .build())
                .createdAt(usuario.getCreatedAt())
                .build();
    }
}
