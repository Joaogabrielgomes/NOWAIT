CREATE TABLE contestacoes_banimento (
    id         CHAR(36)      NOT NULL DEFAULT (UUID()),
    usuario_id CHAR(36)      NOT NULL,
    mensagem   VARCHAR(1000) NOT NULL,
    status     VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_contestacoes_banimento     PRIMARY KEY (id),
    CONSTRAINT fk_contestacoes_banimento_usr FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT chk_contestacoes_banimento_status CHECK (status IN ('PENDENTE', 'ANALISADA'))
);
