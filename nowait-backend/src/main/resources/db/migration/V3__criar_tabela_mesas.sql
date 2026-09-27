CREATE TABLE mesas (
    id                 CHAR(36)     NOT NULL DEFAULT (UUID()),
    estabelecimento_id CHAR(36)     NOT NULL,
    numero             VARCHAR(10)  NOT NULL,
    capacidade         INT          NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'LIVRE',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_mesas                    PRIMARY KEY (id),
    CONSTRAINT fk_mesas_estabelecimento    FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id),
    CONSTRAINT uq_mesas_numero             UNIQUE (estabelecimento_id, numero),
    CONSTRAINT chk_mesas_status            CHECK (status IN ('LIVRE', 'OCUPADA', 'RESERVADA')),
    CONSTRAINT chk_mesas_capacidade        CHECK (capacidade > 0)
);
