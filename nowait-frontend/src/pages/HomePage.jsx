import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { estabelecimentoService } from '../services/api'

export default function HomePage() {
  const [estabelecimentos, setEstabelecimentos] = useState([])
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    estabelecimentoService.listarTodos()
      .then(({ data }) => setEstabelecimentos(data))
      .finally(() => setCarregando(false))
  }, [])

  if (carregando) return <div className="container"><p className="text-muted">Carregando...</p></div>

  return (
    <div className="container">
      <h2>Estabelecimentos</h2>

      {estabelecimentos.length === 0 && (
        <div className="empty-state">Nenhum estabelecimento cadastrado ainda.</div>
      )}

      <div className="grid">
        {estabelecimentos.map((est) => (
          <Link to={`/estabelecimentos/${est.id}`} key={est.id} className="card" style={{ display: 'block' }}>
            <h3>{est.nome}</h3>
            <p className="text-muted">
              {est.endereco.bairro} — {est.endereco.cidade}/{est.endereco.estado}
            </p>
            <span className={`badge ${est.aberto ? 'CHAMADO' : 'CANCELADO'}`}>
              {est.aberto ? 'Aberto' : 'Fechado'}
            </span>
          </Link>
        ))}
      </div>
    </div>
  )
}
