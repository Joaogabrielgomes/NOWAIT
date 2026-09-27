CREATE TABLE fila_espera (
    id                    CHAR(36)     NOT NULL DEFAULT (UUID()),
    estabelecimento_id    CHAR(36)     NOT NULL,
    cliente_id            CHAR(36)     NOT NULL,
    mesa_id               CHAR(36)     NULL,
    quantidade_pessoas    INT          NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'AGUARDANDO',
    hora_entrada          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    hora_chamado          DATETIME     NULL,
    hora_atendimento      DATETIME     NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_fila_espera                 PRIMARY KEY (id),
    CONSTRAINT fk_fila_espera_estabelecimento FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id),
    CONSTRAINT fk_fila_espera_cliente         FOREIGN KEY (cliente_id) REFERENCES usuarios(id),
    CONSTRAINT fk_fila_espera_mesa            FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    CONSTRAINT chk_fila_espera_status         CHECK (status IN ('AGUARDANDO', 'CHAMADO', 'ATENDIDO', 'CANCELADO')),
    CONSTRAINT chk_fila_espera_qtd_pessoas    CHECK (quantidade_pessoas > 0)
);

CREATE INDEX idx_fila_espera_estabelecimento_status ON fila_espera (estabelecimento_id, status);
