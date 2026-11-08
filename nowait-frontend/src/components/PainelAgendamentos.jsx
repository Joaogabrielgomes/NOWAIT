import { useEffect, useState } from 'react'
import { agendamentoService, extrairMensagemErro } from '../services/api'
import StatusBadge from './StatusBadge'
import MotivoModal from './MotivoModal'

function formatarData(iso) {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

export default function PainelAgendamentos({ estabelecimentoId }) {
  const [agendamentos, setAgendamentos] = useState([])
  const [erro, setErro] = useState('')
  const [acaoEmCurso, setAcaoEmCurso] = useState(null)
  const [aba, setAba] = useState('pendentes')
  const [recusandoAg, setRecusandoAg] = useState(null)
  const [cancelandoAg, setCancelandoAg] = useState(null)

  const carregar = () => {
    agendamentoService.listarPorEstabelecimento(estabelecimentoId).then(({ data }) => setAgendamentos(data))
  }

  useEffect(() => { carregar() }, [estabelecimentoId])

  const executar = async (acao, id) => {
    setErro('')
    setAcaoEmCurso(id)
    try {
      if (acao === 'aceitar') await agendamentoService.aceitar(id)
      if (acao === 'confirmar') await agendamentoService.confirmarChegada(id)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível executar a ação'))
    } finally {
      setAcaoEmCurso(null)
    }
  }

  const confirmarRecusa = async (motivo) => {
    const id = recusandoAg.id
    setErro('')
    setAcaoEmCurso(id)
    try {
      await agendamentoService.recusar(id, motivo)
      setRecusandoAg(null)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível recusar o agendamento'))
    } finally {
      setAcaoEmCurso(null)
    }
  }

  const confirmarCancelamento = async (motivo) => {
    const id = cancelandoAg.id
    setErro('')
    setAcaoEmCurso(id)
    try {
      await agendamentoService.cancelar(id, motivo)
      setCancelandoAg(null)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível cancelar o agendamento'))
    } finally {
      setAcaoEmCurso(null)
    }
  }

  const pendentes = agendamentos.filter((ag) => ag.status === 'PENDENTE')
  const ativos = agendamentos.filter((ag) => ag.status === 'CONFIRMADO')
  const historico = agendamentos.filter((ag) => ag.status !== 'PENDENTE' && ag.status !== 'CONFIRMADO')
  const listaExibida = aba === 'pendentes' ? pendentes : aba === 'ativos' ? ativos : historico

  return (
    <div>
      {erro && <div className="error-box">{erro}</div>}

      <div className="btn-row mb-16">
        <button className={aba === 'pendentes' ? '' : 'secondary'} onClick={() => setAba('pendentes')}>
          Pendentes ({pendentes.length})
        </button>
        <button className={aba === 'ativos' ? '' : 'secondary'} onClick={() => setAba('ativos')}>
          Confirmados ({ativos.length})
        </button>
        <button className={aba === 'historico' ? '' : 'secondary'} onClick={() => setAba('historico')}>
          Histórico ({historico.length})
        </button>
      </div>

      {listaExibida.length === 0 ? (
        <div className="empty-state">
          {aba === 'pendentes' && 'Nenhuma solicitação de agendamento pendente.'}
          {aba === 'ativos' && 'Nenhum agendamento confirmado no momento.'}
          {aba === 'historico' && 'Nenhum agendamento no histórico ainda.'}
        </div>
      ) : (
        <table>
          <thead>
            <tr><th>Cliente</th><th>Data/hora</th><th>Pessoas</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {listaExibida.map((ag) => (
              <tr key={ag.id}>
                <td>{ag.clienteNome}</td>
                <td>{formatarData(ag.dataHora)}</td>
                <td>{ag.quantidadePessoas}</td>
                <td><StatusBadge status={ag.status} /></td>
                <td className="btn-row">
                  {ag.status === 'PENDENTE' && (
                    <>
                      <button className="small" onClick={() => executar('aceitar', ag.id)} disabled={acaoEmCurso === ag.id}>
                        Aceitar
                      </button>
                      <button className="danger small" onClick={() => setRecusandoAg(ag)} disabled={acaoEmCurso === ag.id}>
                        Recusar
                      </button>
                    </>
                  )}
                  {ag.status === 'CONFIRMADO' && (
                    <>
                      <button className="small" onClick={() => executar('confirmar', ag.id)} disabled={acaoEmCurso === ag.id}>
                        Confirmar chegada
                      </button>
                      <button className="secondary small" onClick={() => setCancelandoAg(ag)} disabled={acaoEmCurso === ag.id}>
                        Cancelar
                      </button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {recusandoAg && (
        <MotivoModal
          titulo="Recusar agendamento"
          mensagem={`Explique ao cliente ${recusandoAg.clienteNome} por que o agendamento de ${formatarData(recusandoAg.dataHora)} está sendo recusado. Ele receberá um e-mail com esse motivo.`}
          placeholder="Ex.: estabelecimento sem mesas compatíveis nesse horário"
          textoConfirmar="Recusar"
          confirmando={acaoEmCurso === recusandoAg.id}
          onConfirmar={confirmarRecusa}
          onCancelar={() => setRecusandoAg(null)}
        />
      )}

      {cancelandoAg && (
        <MotivoModal
          titulo="Cancelar agendamento"
          mensagem={`Explique ao cliente ${cancelandoAg.clienteNome} por que o agendamento de ${formatarData(cancelandoAg.dataHora)} está sendo cancelado. Ele receberá um e-mail com esse motivo.`}
          placeholder="Ex.: imprevisto no estabelecimento"
          textoConfirmar="Cancelar agendamento"
          confirmando={acaoEmCurso === cancelandoAg.id}
          onConfirmar={confirmarCancelamento}
          onCancelar={() => setCancelandoAg(null)}
        />
      )}
    </div>
  )
}
