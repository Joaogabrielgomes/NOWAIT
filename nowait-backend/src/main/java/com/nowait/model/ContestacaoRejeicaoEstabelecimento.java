package com.nowait.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "contestacoes_rejeicao_estabelecimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContestacaoRejeicaoEstabelecimento {

    @Id
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @Column(nullable = false, length = 1000)
    private String mensagem;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = StatusContestacao.PENDENTE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
