const PALETA = [
  '#ef5a1f', '#4f9c3a', '#e8ac1d', '#d1652c', '#82a334',
  '#e2402a', '#dd8f22', '#a89b8c', '#3f8a2e', '#cf4a18',
]

const ROTULOS_ACAO = {
  LOGIN_SUCESSO: 'Login bem-sucedido',
  LOGIN_FALHA: 'Falha no login',
  REGISTRO_CONTA: 'Conta registrada',
  SENHA_RESET_SOLICITADO: 'Redefinição de senha solicitada',
  SENHA_REDEFINIDA: 'Senha redefinida',
  USUARIO_PERFIL_ATUALIZADO: 'Perfil atualizado',
  USUARIO_CONTA_EXCLUIDA: 'Conta excluída',
  USUARIO_CONTA_ANONIMIZADA: 'Conta anonimizada',
  FILA_ENTRADA: 'Entrada na fila',
  FILA_CHAMADA: 'Cliente chamado na fila',
  FILA_CHAMADA_AUTOMATICA: 'Chamada automática na fila',
  FILA_ATENDIMENTO: 'Atendimento na fila',
  FILA_CANCELAMENTO: 'Cancelamento na fila',
  AGENDAMENTO_CRIADO: 'Agendamento criado',
  AGENDAMENTO_ACEITO: 'Agendamento aceito',
  AGENDAMENTO_RECUSADO: 'Agendamento recusado',
  AGENDAMENTO_CANCELADO: 'Agendamento cancelado',
  AGENDAMENTO_CONFIRMADO: 'Agendamento confirmado',
  AGENDAMENTO_EXPIRADO: 'Agendamento expirado',
  MESA_CRIADA: 'Mesa criada',
  MESA_STATUS_ALTERADO: 'Status da mesa alterado',
  MESA_CAPACIDADE_ALTERADA: 'Capacidade da mesa alterada',
  MESA_LIBERADA: 'Mesa liberada',
  MESA_REMOVIDA: 'Mesa removida',
  AVALIACAO_CRIADA: 'Avaliação criada',
  AVALIACAO_EDITADA: 'Avaliação editada',
  ADMIN_USUARIO_REMOVIDO: 'Usuário removido pelo admin',
  ADMIN_USUARIO_BANIDO: 'Usuário banido',
  ADMIN_USUARIO_DESBANIDO: 'Usuário desbanido',
  ADMIN_ESTABELECIMENTO_APROVADO: 'Estabelecimento aprovado',
  ADMIN_ESTABELECIMENTO_REJEITADO: 'Estabelecimento rejeitado',
  DENUNCIA_CRIADA: 'Denúncia criada',
  DENUNCIA_RESOLVIDA: 'Denúncia resolvida',
  CONTESTACAO_BANIMENTO_CRIADA: 'Contestação de banimento criada',
  CONTESTACAO_BANIMENTO_ANALISADA: 'Contestação de banimento analisada',
  CONTESTACAO_REJEICAO_ESTABELECIMENTO_CRIADA: 'Contestação de rejeição criada',
  CONTESTACAO_REJEICAO_ESTABELECIMENTO_ANALISADA: 'Contestação de rejeição analisada',
}

const ROTULOS_ENTIDADE = {
  Usuario: 'Usuário',
  Estabelecimento: 'Estabelecimento',
  Denuncia: 'Denúncia',
  Agendamento: 'Agendamento',
  Avaliacao: 'Avaliação',
  ContestacaoBanimento: 'Contestação de banimento',
  ContestacaoRejeicaoEstabelecimento: 'Contestação de rejeição',
  FilaEspera: 'Fila de espera',
  Mesa: 'Mesa',
}

export function formatarAcao(acao) {
  if (ROTULOS_ACAO[acao]) return ROTULOS_ACAO[acao]
  return acao
    .toLowerCase()
    .split('_')
    .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
    .join(' ')
}

export function formatarEntidade(entidade) {
  if (!entidade) return '—'
  if (ROTULOS_ENTIDADE[entidade]) return ROTULOS_ENTIDADE[entidade]
  return entidade.replace(/([a-z])([A-Z])/g, '$1 $2')
}

function formatarDiaCurto(iso) {
  const [, mes, dia] = iso.split('-')
  return `${dia}/${mes}`
}

export default function GraficoAcoesPorDia({ dados }) {
  if (!dados || dados.length === 0) {
    return <p className="text-muted">Sem atividade registrada no período.</p>
  }

  const dias = [...new Set(dados.map((d) => d.dia))].sort()
  const acoes = [...new Set(dados.map((d) => d.acao))].sort()
  const cores = Object.fromEntries(acoes.map((a, i) => [a, PALETA[i % PALETA.length]]))

  const porDia = dias.map((dia) => {
    const entradas = dados.filter((d) => d.dia === dia)
    const total = entradas.reduce((s, e) => s + e.quantidade, 0)
    return { dia, total, entradas }
  })

  const maxTotal = Math.max(1, ...porDia.map((d) => d.total))

  const larguraSvg = 640
  const alturaSvg = 220
  const margemInferior = 28
  const alturaUtil = alturaSvg - margemInferior - 10
  const larguraBarra = Math.min(40, (larguraSvg - 20) / dias.length - 8)
  const espacamento = (larguraSvg - larguraBarra * dias.length) / (dias.length + 1)

  return (
    <div>
      <svg
        viewBox={`0 0 ${larguraSvg} ${alturaSvg}`}
        width="100%"
        height={alturaSvg}
        role="img"
        aria-label="Gráfico de ações de auditoria por dia e tipo"
      >
        <line x1="0" y1={alturaSvg - margemInferior} x2={larguraSvg} y2={alturaSvg - margemInferior}
              stroke="var(--border)" strokeWidth="1" />

        {porDia.map((d, i) => {
          const x = espacamento + i * (larguraBarra + espacamento)
          let yAcumulado = alturaSvg - margemInferior

          return (
            <g key={d.dia}>
              {d.entradas.map((e) => {
                const altura = (e.quantidade / maxTotal) * alturaUtil
                yAcumulado -= altura
                return (
                  <rect
                    key={e.acao}
                    x={x}
                    y={yAcumulado}
                    width={larguraBarra}
                    height={altura}
                    fill={cores[e.acao]}
                  >
                    <title>{`${formatarAcao(e.acao)}: ${e.quantidade} em ${formatarDiaCurto(d.dia)}`}</title>
                  </rect>
                )
              })}
              {d.total > 0 && (
                <text
                  x={x + larguraBarra / 2}
                  y={yAcumulado - 4}
                  textAnchor="middle"
                  fontSize="10"
                  fontWeight="700"
                  fill="var(--text)"
                >
                  {d.total}
                </text>
              )}
              <text
                x={x + larguraBarra / 2}
                y={alturaSvg - 10}
                textAnchor="middle"
                fontSize="10"
                fill="var(--text-muted)"
              >
                {formatarDiaCurto(d.dia)}
              </text>
            </g>
          )
        })}
      </svg>

      <div className="btn-row" style={{ flexWrap: 'wrap', gap: '10px 16px', marginTop: 8 }}>
        {acoes.map((a) => (
          <span key={a} style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.78rem' }} className="text-muted">
            <span style={{ width: 10, height: 10, borderRadius: 2, background: cores[a], display: 'inline-block' }} />
            {formatarAcao(a)}
          </span>
        ))}
      </div>
    </div>
  )
}
