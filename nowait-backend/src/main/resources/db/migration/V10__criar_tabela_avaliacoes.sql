CREATE TABLE avaliacoes (
    id                  CHAR(36)     NOT NULL DEFAULT (UUID()),
    estabelecimento_id  CHAR(36)     NOT NULL,
    cliente_id          CHAR(36)     NOT NULL,
    fila_id             CHAR(36)     NULL,
    agendamento_id      CHAR(36)     NULL,
    nota                TINYINT      NOT NULL,
    comentario          VARCHAR(500) NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_avaliacoes                 PRIMARY KEY (id),
    CONSTRAINT fk_avaliacoes_estabelecimento FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id),
    CONSTRAINT fk_avaliacoes_cliente         FOREIGN KEY (cliente_id) REFERENCES usuarios(id),
    CONSTRAINT fk_avaliacoes_fila            FOREIGN KEY (fila_id) REFERENCES fila_espera(id) ON DELETE CASCADE,
    CONSTRAINT fk_avaliacoes_agendamento     FOREIGN KEY (agendamento_id) REFERENCES agendamentos(id) ON DELETE CASCADE,
    CONSTRAINT uq_avaliacoes_fila            UNIQUE (fila_id),
    CONSTRAINT uq_avaliacoes_agendamento     UNIQUE (agendamento_id),
    CONSTRAINT chk_avaliacoes_nota           CHECK (nota BETWEEN 1 AND 5),
    CONSTRAINT chk_avaliacoes_origem         CHECK (
        (fila_id IS NOT NULL AND agendamento_id IS NULL) OR
        (fila_id IS NULL AND agendamento_id IS NOT NULL)
    )
);

CREATE INDEX idx_avaliacoes_estabelecimento ON avaliacoes (estabelecimento_id);
