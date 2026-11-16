package com.nowait.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAuditoria {

    @Id
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @Column(name = "usuario_id", length = 36)
    private String usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AcaoAuditoria acao;

    @Column(name = "entidade_tipo", length = 50)
    private String entidadeTipo;

    @Column(name = "entidade_id", length = 36)
    private String entidadeId;

    @Column(length = 500)
    private String detalhes;

    @Column(length = 45)
    private String ip;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
