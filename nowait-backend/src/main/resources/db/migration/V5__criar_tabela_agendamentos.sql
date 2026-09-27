CREATE TABLE agendamentos (
    id                    CHAR(36)     NOT NULL DEFAULT (UUID()),
    estabelecimento_id    CHAR(36)     NOT NULL,
    cliente_id            CHAR(36)     NOT NULL,
    mesa_id               CHAR(36)     NULL,
    quantidade_pessoas    INT          NOT NULL,
    data_hora             DATETIME     NOT NULL,
    tolerancia_minutos    INT          NOT NULL DEFAULT 15,
    status                VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMADO',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_agendamentos                 PRIMARY KEY (id),
    CONSTRAINT fk_agendamentos_estabelecimento FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id),
    CONSTRAINT fk_agendamentos_cliente         FOREIGN KEY (cliente_id) REFERENCES usuarios(id),
    CONSTRAINT fk_agendamentos_mesa            FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    CONSTRAINT chk_agendamentos_status         CHECK (status IN ('CONFIRMADO', 'CONCLUIDO', 'CANCELADO', 'EXPIRADO')),
    CONSTRAINT chk_agendamentos_qtd_pessoas    CHECK (quantidade_pessoas > 0)
);

CREATE INDEX idx_agendamentos_estabelecimento_status ON agendamentos (estabelecimento_id, status);
