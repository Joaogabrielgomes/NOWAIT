import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { filaService, extrairMensagemErro } from '../services/api'
import { conectarFila } from '../services/ws'
import { useAuth } from '../context/AuthContext'
import StatusBadge from '../components/StatusBadge'

export default function MinhaFilaPage() {
  const { usuario } = useAuth()
  const [entrada, setEntrada] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [cancelando, setCancelando] = useState(false)
  const [erro, setErro] = useState('')
  const cleanupRef = useRef(null)

  const carregar = () => {
    filaService.minhaEntrada()
      .then(({ data, status }) => setEntrada(status === 204 ? null : data))
      .catch(() => setEntrada(null))
      .finally(() => setCarregando(false))
  }

  useEffect(() => {
    carregar()
  }, [])

  useEffect(() => {
    if (cleanupRef.current) {
      cleanupRef.current()
      cleanupRef.current = null
    }
    if (entrada?.estabelecimentoId) {
      cleanupRef.current = conectarFila(entrada.estabelecimentoId, (filaAtualizada) => {
        const minha = filaAtualizada.find((f) => f.id === entrada.id)
        if (minha) setEntrada(minha)
      })
    }
    return () => {
      if (cleanupRef.current) cleanupRef.current()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entrada?.estabelecimentoId])

  const cancelar = async () => {
    setCancelando(true)
    setErro('')
    try {
      await filaService.cancelar(entrada.id)
      setEntrada(null)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível cancelar'))
    } finally {
      setCancelando(false)
    }
  }

  if (carregando) return <div className="container"><p className="text-muted">Carregando...</p></div>

  return (
    <div className="container">
      <h2>Minha fila</h2>
      {erro && <div className="error-box">{erro}</div>}

      {!entrada && (
        <div className="empty-state">
          Você não está em nenhuma fila no momento.
          <div className="mt-16"><Link to="/">Ver estabelecimentos</Link></div>
        </div>
      )}

      {entrada && (
        <div className="card">
          <div className="btn-row" style={{ justifyContent: 'space-between' }}>
            <StatusBadge status={entrada.status} />
            <span className="text-muted">{entrada.quantidadePessoas} pessoa(s)</span>
          </div>

          {entrada.status === 'AGUARDANDO' && (
            <div className="position-highlight">
              <div className="number">{entrada.posicao}</div>
              <p className="text-muted">
                posição na fila
                {entrada.tempoEsperaEstimadoMinutos != null
                  ? ` — cerca de ${entrada.tempoEsperaEstimadoMinutos} min de espera`
                  : ' — aguardando ser chamado'}
              </p>
            </div>
          )}

          {entrada.status === 'CHAMADO' && (
            <div className="position-highlight">
              <div className="number" style={{ color: 'var(--success)' }}>Chamado!</div>
              <p className="text-muted">Dirija-se ao balcão de recepção.</p>
            </div>
          )}

          <button className="danger" onClick={cancelar} disabled={cancelando}>
            {cancelando ? 'Cancelando...' : 'Cancelar'}
          </button>
        </div>
      )}
    </div>
  )
}
