CREATE TABLE password_reset_tokens (
    id         CHAR(36)     NOT NULL DEFAULT (UUID()),
    usuario_id CHAR(36)     NOT NULL,
    token      VARCHAR(100) NOT NULL,
    expira_em  DATETIME     NOT NULL,
    usado      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_token  UNIQUE (token),
    CONSTRAINT fk_prt_usuario           FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_prt_usuario_id ON password_reset_tokens (usuario_id);
CREATE INDEX idx_prt_expira_em  ON password_reset_tokens (expira_em);
