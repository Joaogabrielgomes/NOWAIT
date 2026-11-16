package com.nowait.repository;

import com.nowait.model.ContestacaoBanimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContestacaoBanimentoRepository extends JpaRepository<ContestacaoBanimento, String> {

    @Query("SELECT c FROM ContestacaoBanimento c WHERE "
            + "(:status IS NULL OR c.status = :status) AND "
            + "(:nome IS NULL OR LOWER(c.usuario.nome) LIKE LOWER(CONCAT('%', :nome, '%')))")
    Page<ContestacaoBanimento> buscar(@Param("status") String status, @Param("nome") String nome, Pageable pageable);

    @Query("SELECT c FROM ContestacaoBanimento c WHERE "
            + "(:status IS NULL OR c.status = :status) AND "
            + "(:nome IS NULL OR LOWER(c.usuario.nome) LIKE LOWER(CONCAT('%', :nome, '%')))")
    List<ContestacaoBanimento> buscarTodos(@Param("status") String status, @Param("nome") String nome, Sort sort);
}
