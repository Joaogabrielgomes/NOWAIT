const PALETA = [
  '#ff6b35', '#3ecf8e', '#f2b84b', '#5b8def', '#c77dff',
  '#ef5464', '#4dd0e1', '#a0a0a0', '#ffd166', '#06d6a0',
]

export function formatarAcao(acao) {
  return acao
    .toLowerCase()
    .split('_')
    .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
    .join(' ')
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
