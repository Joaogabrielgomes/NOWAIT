import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { estabelecimentoService } from '../services/api'
import PainelMesas from '../components/PainelMesas'
import PainelFila from '../components/PainelFila'
import PainelAgendamentos from '../components/PainelAgendamentos'
import EstabelecimentoForm from '../components/EstabelecimentoForm'

export default function PainelEstabelecimentoPage() {
  const [estabelecimento, setEstabelecimento] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [semEstabelecimento, setSemEstabelecimento] = useState(false)
  const [aba, setAba] = useState('fila')
  const [editando, setEditando] = useState(false)
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    estabelecimentoService.meuEstabelecimento()
      .then(({ data }) => setEstabelecimento(data))
      .catch(() => setSemEstabelecimento(true))
      .finally(() => setCarregando(false))
  }, [])

  const salvarEdicao = async (dto) => {
    setSalvando(true)
    try {
      const { data } = await estabelecimentoService.atualizar(estabelecimento.id, dto)
      setEstabelecimento(data)
      setEditando(false)
    } finally {
      setSalvando(false)
    }
  }

  if (carregando) return <div className="container"><p className="text-muted">Carregando...</p></div>
  if (semEstabelecimento) return <Navigate to="/painel/cadastro" replace />

  return (
    <div className="container">
      {!editando && (
        <>
          <div className="btn-row" style={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <h2 style={{ marginBottom: 4 }}>{estabelecimento.nome}</h2>
              <p className="text-muted" style={{ marginTop: 0 }}>
                {estabelecimento.endereco.bairro} — {estabelecimento.endereco.cidade}/{estabelecimento.endereco.estado}
              </p>
            </div>
            <button className="secondary small" onClick={() => setEditando(true)}>Editar dados</button>
          </div>

          <div className="btn-row mb-16">
            <button className={aba === 'fila' ? '' : 'secondary'} onClick={() => setAba('fila')}>Fila de espera</button>
            <button className={aba === 'mesas' ? '' : 'secondary'} onClick={() => setAba('mesas')}>Mesas</button>
            <button className={aba === 'agendamentos' ? '' : 'secondary'} onClick={() => setAba('agendamentos')}>Agendamentos</button>
          </div>

          {aba === 'fila' && <PainelFila estabelecimentoId={estabelecimento.id} />}
          {aba === 'mesas' && <PainelMesas estabelecimentoId={estabelecimento.id} />}
          {aba === 'agendamentos' && <PainelAgendamentos estabelecimentoId={estabelecimento.id} />}
        </>
      )}

      {editando && (
        <>
          <h2>Editar estabelecimento</h2>
          <div className="card" style={{ maxWidth: 480 }}>
            <EstabelecimentoForm inicial={estabelecimento} aoSalvar={salvarEdicao} salvando={salvando} />
          </div>
          <button className="secondary small mt-16" onClick={() => setEditando(false)} disabled={salvando}>
            Cancelar
          </button>
        </>
      )}
    </div>
  )
}
