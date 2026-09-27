-- Trilha de auditoria. usuario_id é texto livre (sem FK de propósito): o
-- histórico deve permanecer íntegro mesmo se a conta referenciada for
-- excluída ou anonimizada depois.
CREATE TABLE logs_auditoria (
    id             CHAR(36)     NOT NULL DEFAULT (UUID()),
    usuario_id     CHAR(36)     NULL,
    acao           VARCHAR(50)  NOT NULL,
    entidade_tipo  VARCHAR(50)  NULL,
    entidade_id    CHAR(36)     NULL,
    detalhes       VARCHAR(500) NULL,
    ip             VARCHAR(45)  NULL,
    criado_em      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_logs_auditoria PRIMARY KEY (id)
);

CREATE INDEX idx_logs_auditoria_usuario ON logs_auditoria (usuario_id);
CREATE INDEX idx_logs_auditoria_acao_data ON logs_auditoria (acao, criado_em);
