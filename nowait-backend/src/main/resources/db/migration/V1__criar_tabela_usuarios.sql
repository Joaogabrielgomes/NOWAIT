CREATE TABLE usuarios (
    id             CHAR(36)     NOT NULL DEFAULT (UUID()),
    nome           VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    senha_hash     VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL DEFAULT 'CLIENTE',
    cep            VARCHAR(9)   NULL,
    logradouro     VARCHAR(150) NULL,
    numero         VARCHAR(10)  NULL,
    complemento    VARCHAR(100) NULL,
    bairro         VARCHAR(100) NULL,
    cidade         VARCHAR(100) NULL,
    estado         VARCHAR(2)   NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_usuarios          PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_email    UNIQUE (email),
    CONSTRAINT chk_usuarios_role    CHECK (role IN ('CLIENTE', 'ESTABELECIMENTO', 'ADMIN'))
);
