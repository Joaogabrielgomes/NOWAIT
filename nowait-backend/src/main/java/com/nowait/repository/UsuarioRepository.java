package com.nowait.repository;

import com.nowait.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, String id);

    long countByRole(String role);

    @Query(value =
            "SELECT DATE(created_at) AS dia, COUNT(*) AS quantidade " +
            "FROM usuarios " +
            "WHERE created_at >= :desde " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY dia ASC",
            nativeQuery = true)
    List<ContagemPorDiaProjection> contarNovosPorDia(@Param("desde") LocalDateTime desde);
}
