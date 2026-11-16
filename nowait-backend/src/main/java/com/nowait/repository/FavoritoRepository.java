package com.nowait.repository;

import com.nowait.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, String> {

    List<Favorito> findByClienteIdOrderByCreatedAtDesc(String clienteId);

    Optional<Favorito> findByClienteIdAndEstabelecimentoId(String clienteId, String estabelecimentoId);

    boolean existsByClienteIdAndEstabelecimentoId(String clienteId, String estabelecimentoId);
}
