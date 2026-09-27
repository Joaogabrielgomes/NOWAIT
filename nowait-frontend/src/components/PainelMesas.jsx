import { useEffect, useState } from 'react'
import { mesaService, extrairMensagemErro } from '../services/api'
import StatusBadge from './StatusBadge'

export default function PainelMesas({ estabelecimentoId }) {
  const [mesas, setMesas] = useState([])
  const [numero, setNumero] = useState('')
  const [capacidade, setCapacidade] = useState(4)
  const [erro, setErro] = useState('')
  const [criando, setCriando] = useState(false)

  const carregar = () => {
    mesaService.listar(estabelecimentoId).then(({ data }) => setMesas(data))
  }

  useEffect(() => { carregar() }, [estabelecimentoId])

  const criar = async (e) => {
    e.preventDefault()
    setErro('')
    setCriando(true)
    try {
      await mesaService.criar(estabelecimentoId, { numero, capacidade: Number(capacidade) })
      setNumero('')
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível criar a mesa'))
    } finally {
      setCriando(false)
    }
  }

  const marcarStatus = async (mesa, status) => {
    await mesaService.atualizarStatus(mesa.id, status)
    carregar()
  }

  const liberar = async (mesa) => {
    await mesaService.liberar(mesa.id)
    carregar()
  }

  const remover = async (id) => {
    await mesaService.remover(id)
    carregar()
  }

  return (
    <div>
      <div className="card">
        <h3>Nova mesa</h3>
        {erro && <div className="error-box">{erro}</div>}
        <form onSubmit={criar} className="form-row" style={{ alignItems: 'flex-end' }}>
          <div className="form-group">
            <label>Número</label>
            <input value={numero} onChange={(e) => setNumero(e.target.value)} required maxLength={10} />
          </div>
          <div className="form-group">
            <label>Capacidade</label>
            <input type="number" min={1} max={50} value={capacidade} onChange={(e) => setCapacidade(e.target.value)} />
          </div>
          <div className="form-group" style={{ flex: 'none' }}>
            <button type="submit" disabled={criando}>{criando ? 'Criando...' : 'Adicionar'}</button>
          </div>
        </form>
      </div>

      <p className="text-muted mb-16">
        Ao liberar uma mesa, o sistema tenta chamar automaticamente o próximo cliente da fila que caiba nela.
      </p>

      {mesas.length === 0 ? (
        <div className="empty-state">Nenhuma mesa cadastrada ainda.</div>
      ) : (
        <table>
          <thead>
            <tr><th>Mesa</th><th>Capacidade</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {mesas.map((m) => (
              <tr key={m.id}>
                <td>{m.numero}</td>
                <td>{m.capacidade}</td>
                <td><StatusBadge status={m.status} /></td>
                <td className="btn-row">
                  {m.status === 'LIVRE' && (
                    <>
                      <button className="secondary small" onClick={() => marcarStatus(m, 'OCUPADA')}>Marcar ocupada</button>
                      <button className="secondary small" onClick={() => marcarStatus(m, 'RESERVADA')}>Marcar reservada</button>
                    </>
                  )}
                  {m.status !== 'LIVRE' && (
                    <button className="small" onClick={() => liberar(m)}>Liberar mesa</button>
                  )}
                  <button className="danger small" onClick={() => remover(m.id)}>Remover</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
