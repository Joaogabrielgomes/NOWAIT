import { useEffect, useState } from 'react'
import { agendamentoService, extrairMensagemErro } from '../services/api'
import StatusBadge from './StatusBadge'

function formatarData(iso) {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

export default function PainelAgendamentos({ estabelecimentoId }) {
  const [agendamentos, setAgendamentos] = useState([])
  const [erro, setErro] = useState('')
  const [acaoEmCurso, setAcaoEmCurso] = useState(null)

  const carregar = () => {
    agendamentoService.listarPorEstabelecimento(estabelecimentoId).then(({ data }) => setAgendamentos(data))
  }

  useEffect(() => { carregar() }, [estabelecimentoId])

  const executar = async (acao, id) => {
    setErro('')
    setAcaoEmCurso(id)
    try {
      if (acao === 'confirmar') await agendamentoService.confirmarChegada(id)
      if (acao === 'cancelar') await agendamentoService.cancelar(id)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível executar a ação'))
    } finally {
      setAcaoEmCurso(null)
    }
  }

  if (agendamentos.length === 0) return <div className="empty-state">Nenhum agendamento ainda.</div>

  return (
    <div>
      {erro && <div className="error-box">{erro}</div>}
      <table>
        <thead>
          <tr><th>Cliente</th><th>Data/hora</th><th>Pessoas</th><th>Status</th><th></th></tr>
        </thead>
        <tbody>
          {agendamentos.map((ag) => (
            <tr key={ag.id}>
              <td>{ag.clienteNome}</td>
              <td>{formatarData(ag.dataHora)}</td>
              <td>{ag.quantidadePessoas}</td>
              <td><StatusBadge status={ag.status} /></td>
              <td className="btn-row">
                {ag.status === 'CONFIRMADO' && (
                  <>
                    <button className="small" onClick={() => executar('confirmar', ag.id)} disabled={acaoEmCurso === ag.id}>
                      Confirmar chegada
                    </button>
                    <button className="secondary small" onClick={() => executar('cancelar', ag.id)} disabled={acaoEmCurso === ag.id}>
                      Cancelar
                    </button>
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
