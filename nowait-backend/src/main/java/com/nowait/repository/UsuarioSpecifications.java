package com.nowait.repository;

import com.nowait.model.Usuario;
import org.springframework.data.jpa.domain.Specification;

public final class UsuarioSpecifications {

    private UsuarioSpecifications() {
    }

    public static Specification<Usuario> comRole(String role) {
        if (role == null || role.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    public static Specification<Usuario> comNome(String nome) {
        if (nome == null || nome.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
    }
}
