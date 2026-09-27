package com.nowait.repository;

import com.nowait.model.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, String> {

    Optional<Estabelecimento> findByUsuarioId(String usuarioId);

    boolean existsByUsuarioId(String usuarioId);
}
