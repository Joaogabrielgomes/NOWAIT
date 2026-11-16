CREATE TABLE denuncias (
    id                  CHAR(36)     NOT NULL DEFAULT (UUID()),
    denunciante_id      CHAR(36)     NOT NULL,
    tipo_alvo           VARCHAR(20)  NOT NULL,
    alvo_id             CHAR(36)     NOT NULL,
    motivo              VARCHAR(500) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    resolvido_por_id    CHAR(36)     NULL,
    observacao_admin    VARCHAR(500) NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolvido_em        DATETIME     NULL,

    CONSTRAINT pk_denuncias               PRIMARY KEY (id),
    CONSTRAINT fk_denuncias_denunciante   FOREIGN KEY (denunciante_id) REFERENCES usuarios(id),
    CONSTRAINT fk_denuncias_resolvido_por FOREIGN KEY (resolvido_por_id) REFERENCES usuarios(id),
    CONSTRAINT chk_denuncias_tipo_alvo    CHECK (tipo_alvo IN ('ESTABELECIMENTO', 'AVALIACAO', 'USUARIO')),
    CONSTRAINT chk_denuncias_status       CHECK (status IN ('PENDENTE', 'APROVADA', 'REJEITADA'))
);

CREATE INDEX idx_denuncias_status ON denuncias (status);
CREATE INDEX idx_denuncias_denunciante ON denuncias (denunciante_id);
