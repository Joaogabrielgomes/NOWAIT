package com.nowait.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "estabelecimentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estabelecimento {

    @Id
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 30)
    private String categoria;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(nullable = false, length = 9)
    private String cep;

    @Column(nullable = false, length = 150)
    private String logradouro;

    @Column(nullable = false, length = 10)
    private String numero;

    @Column(length = 100)
    private String complemento;

    @Column(nullable = false, length = 100)
    private String bairro;

    @Column(nullable = false, length = 100)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String estado;

    @Column(name = "tempo_medio_atendimento_minutos", nullable = false)
    @Builder.Default
    private Integer tempoMedioAtendimentoMinutos = 15;

    @Column(name = "tolerancia_padrao_minutos", nullable = false)
    @Builder.Default
    private Integer toleranciaPadraoMinutos = 15;

    @Column(name = "horario_abertura", nullable = false)
    @Builder.Default
    private LocalTime horarioAbertura = LocalTime.of(8, 0);

    @Column(name = "horario_fechamento", nullable = false)
    @Builder.Default
    private LocalTime horarioFechamento = LocalTime.of(22, 0);

    @Column(name = "antecedencia_maxima_dias", nullable = false)
    @Builder.Default
    private Integer antecedenciaMaximaDias = 7;

    @Column(name = "antecedencia_reserva_mesa_minutos", nullable = false)
    @Builder.Default
    private Integer antecedenciaReservaMesaMinutos = 15;

    @Column(name = "antecedencia_minima_minutos", nullable = false)
    @Builder.Default
    private Integer antecedenciaMinimaMinutos = 30;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aberto = true;

    @Column(name = "status_aprovacao", nullable = false, length = 20)
    @Builder.Default
    private String statusAprovacao = StatusAprovacaoEstabelecimento.PENDENTE;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "banner_url", length = 255)
    private String bannerUrl;

    @Column(name = "motivo_rejeicao", length = 500)
    private String motivoRejeicao;

    @Column(name = "token_contestacao_rejeicao", length = 36)
    private String tokenContestacaoRejeicao;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
