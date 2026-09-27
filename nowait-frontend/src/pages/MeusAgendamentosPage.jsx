import { useEffect, useState } from 'react'
import { agendamentoService, extrairMensagemErro } from '../services/api'
import StatusBadge from '../components/StatusBadge'

function formatarData(iso) {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

export default function MeusAgendamentosPage() {
  const [agendamentos, setAgendamentos] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [cancelandoId, setCancelandoId] = useState(null)

  const carregar = () => {
    agendamentoService.listarMeus()
      .then(({ data }) => setAgendamentos(data))
      .finally(() => setCarregando(false))
  }

  useEffect(() => { carregar() }, [])

  const cancelar = async (id) => {
    setCancelandoId(id)
    setErro('')
    try {
      await agendamentoService.cancelar(id)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível cancelar'))
    } finally {
      setCancelandoId(null)
    }
  }

  if (carregando) return <div className="container"><p className="text-muted">Carregando...</p></div>

  return (
    <div className="container">
      <h2>Meus agendamentos</h2>
      {erro && <div className="error-box">{erro}</div>}

      {agendamentos.length === 0 && <div className="empty-state">Nenhum agendamento ainda.</div>}

      {agendamentos.map((ag) => (
        <div className="card" key={ag.id}>
          <div className="btn-row" style={{ justifyContent: 'space-between' }}>
            <h3 style={{ margin: 0 }}>{ag.estabelecimentoNome}</h3>
            <StatusBadge status={ag.status} />
          </div>
          <p className="text-muted">
            {formatarData(ag.dataHora)} — {ag.quantidadePessoas} pessoa(s) — tolerância de {ag.toleranciaMinutos} min
          </p>
          {ag.status === 'CONFIRMADO' && (
            <button className="danger small" onClick={() => cancelar(ag.id)} disabled={cancelandoId === ag.id}>
              {cancelandoId === ag.id ? 'Cancelando...' : 'Cancelar'}
            </button>
          )}
        </div>
      ))}
    </div>
  )
}
