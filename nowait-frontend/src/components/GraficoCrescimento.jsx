const COR_USUARIOS = '#5b8def'
const COR_ESTABELECIMENTOS = '#3ecf8e'

function formatarDiaCurto(iso) {
  const [, mes, dia] = iso.split('-')
  return `${dia}/${mes}`
}

export default function GraficoCrescimento({ dados }) {
  if (!dados || dados.length === 0) {
    return <p className="text-muted">Sem cadastros no período.</p>
  }

  const maxValor = Math.max(1, ...dados.flatMap((d) => [d.novosUsuarios, d.novosEstabelecimentos]))

  const larguraSvg = 640
  const alturaSvg = 220
  const margemInferior = 28
  const alturaUtil = alturaSvg - margemInferior - 10
  const larguraGrupo = Math.min(40, (larguraSvg - 20) / dados.length - 8)
  const larguraBarra = larguraGrupo / 2
  const espacamento = (larguraSvg - larguraGrupo * dados.length) / (dados.length + 1)
  const baseY = alturaSvg - margemInferior

  return (
    <div>
      <svg
        viewBox={`0 0 ${larguraSvg} ${alturaSvg}`}
        width="100%"
        height={alturaSvg}
        role="img"
        aria-label="Gráfico de novos cadastros por dia"
      >
        <line x1="0" y1={baseY} x2={larguraSvg} y2={baseY} stroke="var(--border)" strokeWidth="1" />

        {dados.map((d, i) => {
          const x = espacamento + i * (larguraGrupo + espacamento)
          const alturaUsuarios = (d.novosUsuarios / maxValor) * alturaUtil
          const alturaEstabelecimentos = (d.novosEstabelecimentos / maxValor) * alturaUtil

          return (
            <g key={d.dia}>
              <rect x={x} y={baseY - alturaUsuarios} width={larguraBarra} height={alturaUsuarios} fill={COR_USUARIOS}>
                <title>{`Novos usuários: ${d.novosUsuarios} em ${formatarDiaCurto(d.dia)}`}</title>
              </rect>
              <rect
                x={x + larguraBarra}
                y={baseY - alturaEstabelecimentos}
                width={larguraBarra}
                height={alturaEstabelecimentos}
                fill={COR_ESTABELECIMENTOS}
              >
                <title>{`Novos estabelecimentos: ${d.novosEstabelecimentos} em ${formatarDiaCurto(d.dia)}`}</title>
              </rect>
              <text x={x + larguraGrupo / 2} y={alturaSvg - 10} textAnchor="middle" fontSize="10" fill="var(--text-muted)">
                {formatarDiaCurto(d.dia)}
              </text>
            </g>
          )
        })}
      </svg>

      <div className="btn-row" style={{ flexWrap: 'wrap', gap: '10px 16px', marginTop: 8 }}>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.78rem' }} className="text-muted">
          <span style={{ width: 10, height: 10, borderRadius: 2, background: COR_USUARIOS, display: 'inline-block' }} />
          Novos usuários
        </span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.78rem' }} className="text-muted">
          <span style={{ width: 10, height: 10, borderRadius: 2, background: COR_ESTABELECIMENTOS, display: 'inline-block' }} />
          Novos estabelecimentos
        </span>
      </div>
    </div>
  )
}
