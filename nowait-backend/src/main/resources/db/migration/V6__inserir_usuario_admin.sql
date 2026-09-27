-- Usuario administrador padrao do sistema.
-- Senha inicial: Admin@123 (hash BCrypt abaixo). Deve ser trocada apos o primeiro acesso.
INSERT INTO usuarios (id, nome, email, senha_hash, role)
VALUES ('00000000-0000-0000-0000-000000000001', 'Administrador NOWAIT', 'admin@nowait.com',
        '$2b$10$6gV1EzMpBxD9Bey4U4eRqOXJPLj5GPh6QShsbbR/66QzxvizQybFi', 'ADMIN');
