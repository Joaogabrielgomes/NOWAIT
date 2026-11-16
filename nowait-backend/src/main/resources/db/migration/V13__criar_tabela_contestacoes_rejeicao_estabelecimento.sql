CREATE TABLE contestacoes_rejeicao_estabelecimento (
    id                 CHAR(36)      NOT NULL DEFAULT (UUID()),
    estabelecimento_id CHAR(36)      NOT NULL,
    mensagem           VARCHAR(1000) NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_contestacoes_rejeicao_estabelecimento     PRIMARY KEY (id),
    CONSTRAINT fk_contestacoes_rejeicao_estabelecimento_est FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id),
    CONSTRAINT chk_contestacoes_rejeicao_estabelecimento_status CHECK (status IN ('PENDENTE', 'ANALISADA'))
);
