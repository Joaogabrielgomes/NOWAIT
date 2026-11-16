CREATE TABLE favoritos (
    id                  CHAR(36)  NOT NULL DEFAULT (UUID()),
    cliente_id          CHAR(36)  NOT NULL,
    estabelecimento_id  CHAR(36)  NOT NULL,
    created_at          DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_favoritos                 PRIMARY KEY (id),
    CONSTRAINT fk_favoritos_cliente         FOREIGN KEY (cliente_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_favoritos_estabelecimento FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos (id) ON DELETE CASCADE,
    CONSTRAINT uq_favoritos_cliente_estab   UNIQUE (cliente_id, estabelecimento_id)
);

CREATE INDEX idx_favoritos_cliente ON favoritos (cliente_id);
