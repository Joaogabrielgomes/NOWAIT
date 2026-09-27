# NOWAIT — Backend

Sistema de gestão de acessos a restaurantes: fila de espera virtual em tempo
real, agendamentos com cancelamento automático por tolerância e alocação
otimizada de mesas.

Primeira versão do backend (TCC — Sistemas de Informação, UMC).

## Stack

- Java 21 + Spring Boot 3.5
- Spring Security + JWT (JSON Web Tokens) com BCrypt
- Spring Data JPA (Hibernate) + MySQL
- Flyway (migrations)
- Spring WebSocket (STOMP/SockJS) — atualizações em tempo real da fila
- Bucket4j + Caffeine — rate limiting
- ViaCEP — autocompletamento de endereço no cadastro

## Camadas

```
config/      beans de infraestrutura (security, CORS, WebSocket, RestTemplate)
controller/  endpoints REST
dto/         objetos de entrada/saída da API
exception/   exceções de negócio + tratamento global (GlobalExceptionHandler)
filter/      rate limiting (por IP e por usuário/rota sensível)
model/       entidades JPA
repository/  acesso a dados (Spring Data JPA)
security/    JWT (geração/validação) e filtro de autenticação
service/     regras de negócio
```

## Como rodar

1. Crie o banco no MySQL:
   ```sql
   CREATE DATABASE nowait CHARACTER SET utf8mb4;
   ```
2. Ajuste `src/main/resources/application-local.yml` com usuário/senha do seu MySQL.
3. Rode a aplicação (as migrations do Flyway criam as tabelas automaticamente):
   ```
   ./mvnw spring-boot:run
   ```
4. A API sobe em `http://localhost:8080`.

## Usuário administrador padrão

Criado pela migration `V6__inserir_usuario_admin.sql`:

- **email:** `admin@nowait.com`
- **senha:** `Admin@123`

Troque essa senha assim que possível (o reset de senha por e-mail fica para
uma próxima etapa — ver "Deixado para depois").

## Perfis de usuário

- `CLIENTE` — entra na fila, faz agendamentos.
- `ESTABELECIMENTO` — dono/gerente do restaurante; gerencia mesas, fila e agendamentos do seu estabelecimento (1 estabelecimento por usuário nesta versão).
- `ADMIN` — administração geral (usuários e estabelecimentos).

## Principais endpoints

```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/forgot-password
POST   /api/auth/reset-password
GET    /api/usuarios/me

GET    /api/cep/{cep}

POST   /api/estabelecimentos                (ESTABELECIMENTO)
PUT    /api/estabelecimentos/{id}           (dono)
GET    /api/estabelecimentos
GET    /api/estabelecimentos/me             (ESTABELECIMENTO)
GET    /api/estabelecimentos/{id}

POST   /api/estabelecimentos/{id}/mesas     (dono)
GET    /api/estabelecimentos/{id}/mesas
PUT    /api/mesas/{id}                      (dono)
DELETE /api/mesas/{id}                      (dono)

POST   /api/estabelecimentos/{id}/fila      (CLIENTE)
GET    /api/estabelecimentos/{id}/fila      (dono)
GET    /api/fila/me/ativa                   (CLIENTE)
PATCH  /api/fila/{id}/chamar                (dono)
PATCH  /api/fila/{id}/atender               (dono)
PATCH  /api/fila/{id}/cancelar              (CLIENTE ou dono)

POST   /api/estabelecimentos/{id}/agendamentos      (CLIENTE)
GET    /api/estabelecimentos/{id}/agendamentos      (dono)
GET    /api/agendamentos/me                         (CLIENTE)
PATCH  /api/agendamentos/{id}/cancelar               (CLIENTE ou dono)
PATCH  /api/agendamentos/{id}/confirmar-chegada      (dono)

GET    /api/admin/usuarios          (ADMIN)
DELETE /api/admin/usuarios/{id}     (ADMIN)
GET    /api/admin/estabelecimentos  (ADMIN)
GET    /api/admin/auditoria         (ADMIN) — filtros opcionais: usuarioId, acao, pagina, tamanho
```

## Tempo real (WebSocket)

Endpoint STOMP/SockJS: `ws://localhost:8080/ws`
Tópico de atualização da fila de um estabelecimento: `/topic/fila.{estabelecimentoId}`

Toda escrita (entrar na fila, chamar, atender, cancelar) acontece via REST
autenticado por JWT; o WebSocket é usado apenas para transmitir o estado
atualizado da fila aos clientes conectados.

## Algoritmo de alocação de mesas

`AlocacaoMesaService` usa a estratégia **best-fit**: entre as mesas livres
com capacidade suficiente para o grupo, escolhe a de menor capacidade que
ainda comporte o grupo — minimizando assentos ociosos. Não há, nesta
primeira versão, junção de múltiplas mesas para um único grupo.

## Auditoria

Toda ação sensível do sistema (login, cadastro, entrada/chamada/cancelamento
na fila, agendamentos, mesas, remoção de usuário pelo admin) gera um registro
em `logs_auditoria` — quem fez, o quê, quando e de qual IP. Nunca grava senha,
token ou dado sensível, só um resumo da ação. Consultável por qualquer ADMIN
em `GET /api/admin/auditoria`. Logs com mais de 12 meses são apagados
automaticamente todo dia às 3h (`AuditoriaService.limparLogsAntigos`).

Uma falha ao gravar um log nunca derruba a operação que está sendo auditada —
o erro é apenas registrado no console.

## LGPD — Termos de Uso e Política de Privacidade

O cadastro (`UsuarioRegistroDTO.aceiteTermos`) exige a confirmação de aceite
dos Termos de Uso e da Política de Privacidade antes de criar a conta. O
aceite fica registrado na trilha de auditoria (`REGISTRO_CONTA`).

## Reset de senha — já no código, precisa só de um Gmail com senha de app

`POST /api/auth/forgot-password` gera um token de uso único (válido por 1
hora) e envia por e-mail um link de redefinição. `POST /api/auth/reset-password`
troca a senha mediante esse token. Por segurança, `forgot-password` sempre
responde 200, exista ou não o e-mail cadastrado — evita confirmar quais
e-mails têm conta no sistema.

Como configurar:

1. No Gmail que vai enviar os e-mails (`nowait.noreply@gmail.com`, ou o que você preferir), ative a verificação em duas etapas (obrigatória para gerar senha de app).
2. Acesse https://myaccount.google.com/apppasswords e gere uma **senha de app** (16 caracteres, sem espaços).
3. Preencha em `application-local.yml`:
   ```yaml
   spring:
     mail:
       username: nowait.noreply@gmail.com
       password: <senha de app gerada>
   ```
4. Reinicie a aplicação.

O link do e-mail aponta para `${app.url}/reset-password?token=...` —
`app.url` já está configurado como `http://localhost:5173` (o frontend) em
`application-local.yml`.

Sem a senha de app configurada, o envio falha com um erro tratado (não
derruba a aplicação, só a solicitação específica de reset).

## Deixado para depois

- Dashboard/estatísticas administrativas
