package com.nowait.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "denuncias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Denuncia {

    @Id
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denunciante_id", nullable = false)
    private Usuario denunciante;

    @Column(name = "tipo_alvo", nullable = false, length = 20)
    private String tipoAlvo;

    @Column(name = "alvo_id", nullable = false, length = 36)
    private String alvoId;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = StatusDenuncia.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolvido_por_id")
    private Usuario resolvidoPor;

    @Column(name = "observacao_admin", length = 500)
    private String observacaoAdmin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolvido_em")
    private LocalDateTime resolvidoEm;
}
