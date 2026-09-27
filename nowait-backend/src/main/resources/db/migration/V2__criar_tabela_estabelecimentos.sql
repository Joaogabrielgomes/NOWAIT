CREATE TABLE estabelecimentos (
    id                              CHAR(36)     NOT NULL DEFAULT (UUID()),
    usuario_id                      CHAR(36)     NOT NULL,
    nome                            VARCHAR(150) NOT NULL,
    cep                             VARCHAR(9)   NOT NULL,
    logradouro                      VARCHAR(150) NOT NULL,
    numero                          VARCHAR(10)  NOT NULL,
    complemento                     VARCHAR(100) NULL,
    bairro                          VARCHAR(100) NOT NULL,
    cidade                          VARCHAR(100) NOT NULL,
    estado                          VARCHAR(2)   NOT NULL,
    tempo_medio_atendimento_minutos INT          NOT NULL DEFAULT 15,
    aberto                          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_estabelecimentos            PRIMARY KEY (id),
    CONSTRAINT uq_estabelecimentos_usuario_id UNIQUE (usuario_id),
    CONSTRAINT fk_estabelecimentos_usuario    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
