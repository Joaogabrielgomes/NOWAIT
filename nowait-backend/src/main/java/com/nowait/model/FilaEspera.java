package com.nowait.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "fila_espera")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilaEspera {

    @Id
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;

    @Column(name = "quantidade_pessoas", nullable = false)
    private Integer quantidadePessoas;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = StatusFila.AGUARDANDO;

    @Column(name = "hora_entrada", nullable = false)
    @Builder.Default
    private LocalDateTime horaEntrada = LocalDateTime.now();

    @Column(name = "hora_chamado")
    private LocalDateTime horaChamado;

    @Column(name = "hora_atendimento")
    private LocalDateTime horaAtendimento;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
