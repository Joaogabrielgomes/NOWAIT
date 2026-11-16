package com.nowait.repository;

import com.nowait.model.Mesa;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MesaRepository extends JpaRepository<Mesa, String> {

    List<Mesa> findByEstabelecimentoIdOrderByNumeroAsc(String estabelecimentoId);

    List<Mesa> findByEstabelecimentoIdAndStatusOrderByCapacidadeAsc(String estabelecimentoId, String status);

    boolean existsByEstabelecimentoIdAndNumero(String estabelecimentoId, String numero);

    long countByEstabelecimentoId(String estabelecimentoId);

    long countByEstabelecimentoIdAndStatus(String estabelecimentoId, String status);

    @Query("SELECT MAX(m.capacidade) FROM Mesa m WHERE m.estabelecimento.id = :estabelecimentoId")
    Integer buscarMaiorCapacidade(String estabelecimentoId);

    @Query("SELECT MIN(m.capacidade) FROM Mesa m WHERE m.estabelecimento.id = :estabelecimentoId")
    Integer buscarMenorCapacidade(String estabelecimentoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT m FROM Mesa m WHERE m.estabelecimento.id = :estabelecimentoId AND m.status = :status ORDER BY m.capacidade ASC")
    List<Mesa> buscarLivresParaAlocacaoComLock(String estabelecimentoId, String status);
}
