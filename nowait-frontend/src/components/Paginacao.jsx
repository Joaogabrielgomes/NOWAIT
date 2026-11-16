export default function Paginacao({ paginaAtual, totalPaginas, aoMudar, ariaLabel = 'Paginação' }) {
  if (totalPaginas <= 1) return null

  const janela = 2
  const paginas = []
  for (let p = 1; p <= totalPaginas; p++) {
    if (p === 1 || p === totalPaginas || Math.abs(p - paginaAtual) <= janela) {
      paginas.push(p)
    } else if (paginas[paginas.length - 1] !== '...') {
      paginas.push('...')
    }
  }

  return (
    <nav className="pagination" aria-label={ariaLabel}>
      <button
        className="secondary small"
        onClick={() => aoMudar(paginaAtual - 1)}
        disabled={paginaAtual === 1}
      >
        ‹
      </button>
      {paginas.map((p, i) =>
        p === '...' ? (
          <span key={`dots-${i}`} className="pagination-dots">…</span>
        ) : (
          <button
            key={p}
            className={p === paginaAtual ? '' : 'secondary'}
            onClick={() => aoMudar(p)}
          >
            {p}
          </button>
        )
      )}
      <button
        className="secondary small"
        onClick={() => aoMudar(paginaAtual + 1)}
        disabled={paginaAtual === totalPaginas}
      >
        ›
      </button>
    </nav>
  )
}
