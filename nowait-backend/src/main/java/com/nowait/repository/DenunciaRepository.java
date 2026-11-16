package com.nowait.repository;

import com.nowait.model.Denuncia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DenunciaRepository extends JpaRepository<Denuncia, String> {

    @Query("SELECT d FROM Denuncia d WHERE "
            + "(:status IS NULL OR d.status = :status) AND "
            + "(:nome IS NULL OR LOWER(d.denunciante.nome) LIKE LOWER(CONCAT('%', :nome, '%')))")
    Page<Denuncia> buscar(@Param("status") String status, @Param("nome") String nome, Pageable pageable);
}
