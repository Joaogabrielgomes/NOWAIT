package com.nowait.repository;

import com.nowait.model.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, String> {

    List<Mesa> findByEstabelecimentoIdOrderByNumeroAsc(String estabelecimentoId);

    List<Mesa> findByEstabelecimentoIdAndStatusOrderByCapacidadeAsc(String estabelecimentoId, String status);

    Optional<Mesa> findByEstabelecimentoIdAndNumero(String estabelecimentoId, String numero);

    boolean existsByEstabelecimentoIdAndNumero(String estabelecimentoId, String numero);
}
