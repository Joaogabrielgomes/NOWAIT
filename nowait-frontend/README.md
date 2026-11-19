# NOWAIT — Frontend

SPA em React (Vite) para o sistema NOWAIT de gestão de filas de espera e
agendamentos em restaurantes.

## Stack

- React 18 + React Router
- Axios (consumo da API REST do backend)
- @stomp/stompjs + sockjs-client (fila de espera em tempo real via WebSocket)

## Como rodar

```
npm install
npm run dev
```

A aplicação sobe em `http://localhost:5173` e usa proxy (configurado em
`vite.config.js`) para `/api` e `/ws` apontando para `http://localhost:8080`
(o backend do NOWAIT).

## Estrutura

```
src/
  components/   componentes reutilizáveis (Navbar, formulários, abas do painel)
  context/      AuthContext (sessão do usuário logado)
  pages/        páginas/rotas da aplicação
  services/     api.js (Axios) e ws.js (conexão STOMP/SockJS)
```

## Fluxos principais

- **Cliente:** cria conta → navega pelos estabelecimentos → entra na fila ou
  agenda um horário → acompanha a posição na fila em tempo real em
  "Minha fila" → cancela se necessário.
- **Estabelecimento:** cria conta → cadastra o estabelecimento (CEP
  autocompleta o endereço) → gerencia mesas, chama/atende a fila e confirma
  chegada de agendamentos no painel.
- **Admin:** usa o usuário `admin@nowait.com` / `Admin@123` (criado pela
  migration do backend) para listar usuários, estabelecimentos e consultar a
  trilha de auditoria.

## Termos de Uso e Política de Privacidade

No cadastro, o link "Termos de Uso e a Política de Privacidade" abre um popup
com o texto completo. É preciso marcar a caixa de aceite para criar a conta.

## Esqueci minha senha

Fluxo em duas telas: `/esqueci-senha` (informa o e-mail, dispara o link) e
`/reset-password?token=...` (define a nova senha). Depende do backend estar
com o envio de e-mail configurado (ver README do backend).

## Deixado para depois
- Dashboard com métricas/gráficos
- Notificações por e-mail
