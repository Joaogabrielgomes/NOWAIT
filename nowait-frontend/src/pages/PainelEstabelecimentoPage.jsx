import { useEffect, useRef, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { estabelecimentoService } from '../services/api'
import PainelMesas from '../components/PainelMesas'
import PainelFila from '../components/PainelFila'
import PainelAgendamentos from '../components/PainelAgendamentos'

export default function PainelEstabelecimentoPage() {
  const [estabelecimento, setEstabelecimento] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [semEstabelecimento, setSemEstabelecimento] = useState(false)
  const [aba, setAba] = useState('fila')

  useEffect(() => {
    estabelecimentoService.meuEstabelecimento()
      .then(({ data }) => setEstabelecimento(data))
      .catch(() => setSemEstabelecimento(true))
      .finally(() => setCarregando(false))
  }, [])

  if (carregando) return <div className="container"><p className="text-muted">Carregando...</p></div>
  if (semEstabelecimento) return <Navigate to="/painel/cadastro" replace />

  return (
    <div className="container">
      <h2>{estabelecimento.nome}</h2>
      <p className="text-muted">
        {estabelecimento.endereco.bairro} — {estabelecimento.endereco.cidade}/{estabelecimento.endereco.estado}
      </p>

      <div className="btn-row mb-16">
        <button className={aba === 'fila' ? '' : 'secondary'} onClick={() => setAba('fila')}>Fila de espera</button>
        <button className={aba === 'mesas' ? '' : 'secondary'} onClick={() => setAba('mesas')}>Mesas</button>
        <button className={aba === 'agendamentos' ? '' : 'secondary'} onClick={() => setAba('agendamentos')}>Agendamentos</button>
      </div>

      {aba === 'fila' && <PainelFila estabelecimentoId={estabelecimento.id} />}
      {aba === 'mesas' && <PainelMesas estabelecimentoId={estabelecimento.id} />}
      {aba === 'agendamentos' && <PainelAgendamentos estabelecimentoId={estabelecimento.id} />}
    </div>
  )
}
