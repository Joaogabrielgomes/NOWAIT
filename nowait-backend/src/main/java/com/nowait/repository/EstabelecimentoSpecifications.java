package com.nowait.repository;

import com.nowait.model.Estabelecimento;
import org.springframework.data.jpa.domain.Specification;

public final class EstabelecimentoSpecifications {

    private EstabelecimentoSpecifications() {
    }

    public static Specification<Estabelecimento> aprovadoEAberto(String statusAprovacao) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("statusAprovacao"), statusAprovacao),
                cb.isTrue(root.get("aberto")));
    }

    public static Specification<Estabelecimento> comStatusAprovacao(String statusAprovacao) {
        if (statusAprovacao == null || statusAprovacao.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("statusAprovacao"), statusAprovacao);
    }

    public static Specification<Estabelecimento> comNome(String nome) {
        if (nome == null || nome.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
    }

    public static Specification<Estabelecimento> comCidade(String cidade) {
        if (cidade == null || cidade.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("cidade")), "%" + cidade.toLowerCase() + "%");
    }

    public static Specification<Estabelecimento> comBairro(String bairro) {
        if (bairro == null || bairro.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("bairro")), "%" + bairro.toLowerCase() + "%");
    }

    public static Specification<Estabelecimento> comCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("categoria"), categoria);
    }
}
