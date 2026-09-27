package com.nowait.service;

import com.nowait.dto.EnderecoDTO;
import com.nowait.dto.UsuarioResponseDTO;
import com.nowait.exception.ResourceNotFoundException;
import com.nowait.model.Usuario;
import com.nowait.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }

    public Usuario buscarPorId(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", id));
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
