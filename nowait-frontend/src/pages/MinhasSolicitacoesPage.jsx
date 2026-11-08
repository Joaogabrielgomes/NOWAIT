import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { filaService, agendamentoService, avaliacaoService, extrairMensagemErro } from '../services/api'
import { conectarFila } from '../services/ws'
import StatusBadge from '../components/StatusBadge'
import ConfirmModal from '../components/ConfirmModal'
import AvaliacaoModal from '../components/AvaliacaoModal'

const STATUS_ATIVOS_FILA = ['AGUARDANDO', 'CHAMADO']
const STATUS_ATIVOS_AGENDAMENTO = ['PENDENTE', 'CONFIRMADO']

function formatarData(iso) {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

export default function MinhasSolicitacoesPage() {
  const [secao, setSecao] = useState('fila')

  return (
    <div className="container">
      <h2>Minhas solicitações</h2>

      <div className="btn-row mb-16">
        <button className={secao === 'fila' ? '' : 'secondary'} onClick={() => setSecao('fila')}>
          Fila
        </button>
        <button className={secao === 'agendamentos' ? '' : 'secondary'} onClick={() => setSecao('agendamentos')}>
          Agendamentos
        </button>
      </div>

      {secao === 'fila' ? <SecaoFila /> : <SecaoAgendamentos />}
    </div>
  )
}

function SecaoFila() {
  const [aba, setAba] = useState('ativa')
  const [entrada, setEntrada] = useState(null)
  const [historico, setHistorico] = useState([])
  const [pendentesAvaliacao, setPendentesAvaliacao] = useState({})
  const [carregando, setCarregando] = useState(true)
  const [cancelando, setCancelando] = useState(false)
  const [confirmandoCancelamento, setConfirmandoCancelamento] = useState(false)
  const [avaliandoItem, setAvaliandoItem] = useState(null)
  const [avaliando, setAvaliando] = useState(false)
  const [erro, setErro] = useState('')
  const cleanupRef = useRef(null)

  const carregar = () => {
    Promise.all([filaService.minhaEntrada(), filaService.meuHistorico(), avaliacaoService.listarPendentes()])
      .then(([ativa, hist, pendentes]) => {
        setEntrada(ativa.status === 204 ? null : ativa.data)
        setHistorico(hist.data.filter((f) => !STATUS_ATIVOS_FILA.includes(f.status)))

        const mapaPendentes = {}
        pendentes.data.filter((p) => p.tipo === 'FILA').forEach((p) => { mapaPendentes[p.referenciaId] = true })
        setPendentesAvaliacao(mapaPendentes)
      })
      .catch(() => { setEntrada(null); setHistorico([]) })
      .finally(() => setCarregando(false))
  }

  useEffect(() => { carregar() }, [])

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
  }, [entrada?.estabelecimentoId])

  const cancelar = async () => {
    setCancelando(true)
    setErro('')
    try {
      await filaService.cancelar(entrada.id)
      setConfirmandoCancelamento(false)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível cancelar'))
    } finally {
      setCancelando(false)
    }
  }

  const confirmarAvaliacao = async (nota, comentario) => {
    setAvaliando(true)
    setErro('')
    try {
      await avaliacaoService.avaliar({ filaId: avaliandoItem.id, nota, comentario })
      setAvaliandoItem(null)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível enviar a avaliação.'))
    } finally {
      setAvaliando(false)
    }
  }

  if (carregando) return <p className="text-muted">Carregando...</p>

  return (
    <div>
      <div className="btn-row mb-16">
        <button className={aba === 'ativa' ? '' : 'secondary'} onClick={() => setAba('ativa')}>
          Ativa ({entrada ? 1 : 0})
        </button>
        <button className={aba === 'historico' ? '' : 'secondary'} onClick={() => setAba('historico')}>
          Histórico ({historico.length})
        </button>
      </div>

      {erro && <div className="error-box">{erro}</div>}

      {aba === 'ativa' && (
        <>
          {!entrada && (
            <div className="empty-state">
              Você não está em nenhuma fila no momento.
              <div className="mt-16"><Link to="/">Ver estabelecimentos</Link></div>
            </div>
          )}

          {entrada && (
            <div className="card chamado-line">
              <StatusBadge status={entrada.status} />
              <strong>{entrada.estabelecimentoNome}</strong>
              <span className="text-muted">
                {entrada.status === 'CHAMADO'
                  ? `Chamado · ${entrada.quantidadePessoas} pessoa(s)`
                  : `${entrada.posicao}ª posição · ${entrada.quantidadePessoas} pessoa(s)` +
                    (entrada.tempoEsperaEstimadoMinutos != null
                      ? ` · ~${entrada.tempoEsperaEstimadoMinutos} min`
                      : ' · aguardando ser chamado')}
              </span>
              <button
                className="danger small"
                onClick={() => setConfirmandoCancelamento(true)}
                disabled={cancelando}
                style={{ marginLeft: 'auto' }}
              >
                {cancelando ? 'Cancelando...' : 'Cancelar'}
              </button>
            </div>
          )}
        </>
      )}

      {aba === 'historico' && (
        historico.length === 0 ? (
          <div className="empty-state">Nenhum histórico de fila ainda.</div>
        ) : (
          historico.map((f) => {
            const podeAvaliar = pendentesAvaliacao[f.id]
            const atendida = f.status === 'ATENDIDO'
            return (
              <div className="card" key={f.id}>
                <div className="btn-row" style={{ justifyContent: 'space-between' }}>
                  <h3 style={{ margin: 0 }}>{f.estabelecimentoNome}</h3>
                  <StatusBadge status={f.status} />
                </div>
                <p className="text-muted">
                  {formatarData(f.horaEntrada)} — {f.quantidadePessoas} pessoa(s)
                </p>
                {podeAvaliar && (
                  <button className="small" onClick={() => setAvaliandoItem(f)}>Avaliar</button>
                )}
                {!podeAvaliar && atendida && !f.avaliacaoLiberada && (
                  <p className="text-muted" style={{ fontSize: '0.82rem' }}>
                    Aguardando o estabelecimento liberar a mesa para avaliar.
                  </p>
                )}
                {!podeAvaliar && atendida && f.avaliacaoLiberada && (
                  <p className="text-muted" style={{ fontSize: '0.82rem' }}>Avaliado.</p>
                )}
              </div>
            )
          })
        )
      )}

      {confirmandoCancelamento && (
        <ConfirmModal
          titulo="Cancelar posição na fila"
          mensagem={`Tem certeza que deseja sair da fila de ${entrada?.estabelecimentoNome}? Você perderá sua posição.`}
          textoConfirmar="Sim, cancelar"
          textoCancelar="Não"
          confirmando={cancelando}
          onConfirmar={cancelar}
          onCancelar={() => setConfirmandoCancelamento(false)}
        />
      )}

      {avaliandoItem && (
        <AvaliacaoModal
          mensagem={`Como foi sua experiência em ${avaliandoItem.estabelecimentoNome}?`}
          confirmando={avaliando}
          onConfirmar={confirmarAvaliacao}
          onCancelar={() => setAvaliandoItem(null)}
        />
      )}
    </div>
  )
}

function SecaoAgendamentos() {
  const [aba, setAba] = useState('ativos')
  const [agendamentos, setAgendamentos] = useState([])
  const [pendentesAvaliacao, setPendentesAvaliacao] = useState({})
  const [carregando, setCarregando] = useState(true)
  const [cancelandoId, setCancelandoId] = useState(null)
  const [agendamentoParaCancelar, setAgendamentoParaCancelar] = useState(null)
  const [avaliandoItem, setAvaliandoItem] = useState(null)
  const [avaliando, setAvaliando] = useState(false)
  const [erro, setErro] = useState('')

  const carregar = () => {
    Promise.all([agendamentoService.listarMeus(), avaliacaoService.listarPendentes()])
      .then(([ags, pendentes]) => {
        setAgendamentos(ags.data)
        const mapaPendentes = {}
        pendentes.data.filter((p) => p.tipo === 'AGENDAMENTO').forEach((p) => { mapaPendentes[p.referenciaId] = true })
        setPendentesAvaliacao(mapaPendentes)
      })
      .finally(() => setCarregando(false))
  }

  useEffect(() => { carregar() }, [])

  const cancelar = async () => {
    const id = agendamentoParaCancelar.id
    setCancelandoId(id)
    setErro('')
    try {
      await agendamentoService.cancelar(id)
      setAgendamentoParaCancelar(null)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível cancelar'))
    } finally {
      setCancelandoId(null)
    }
  }

  const confirmarAvaliacao = async (nota, comentario) => {
    setAvaliando(true)
    setErro('')
    try {
      await avaliacaoService.avaliar({ agendamentoId: avaliandoItem.id, nota, comentario })
      setAvaliandoItem(null)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível enviar a avaliação.'))
    } finally {
      setAvaliando(false)
    }
  }

  if (carregando) return <p className="text-muted">Carregando...</p>

  const ativos = agendamentos.filter((ag) => STATUS_ATIVOS_AGENDAMENTO.includes(ag.status))
  const historico = agendamentos.filter((ag) => !STATUS_ATIVOS_AGENDAMENTO.includes(ag.status))
  const listaExibida = aba === 'ativos' ? ativos : historico

  return (
    <div>
      <div className="btn-row mb-16">
        <button className={aba === 'ativos' ? '' : 'secondary'} onClick={() => setAba('ativos')}>
          Ativos ({ativos.length})
        </button>
        <button className={aba === 'historico' ? '' : 'secondary'} onClick={() => setAba('historico')}>
          Histórico ({historico.length})
        </button>
      </div>

      {erro && <div className="error-box">{erro}</div>}

      {listaExibida.length === 0 && (
        <div className="empty-state">
          {aba === 'ativos' ? 'Nenhum agendamento ativo no momento.' : 'Nenhum agendamento no histórico ainda.'}
        </div>
      )}

      {listaExibida.map((ag) => {
        const podeAvaliar = pendentesAvaliacao[ag.id]
        const concluido = ag.status === 'CONCLUIDO'
        return (
          <div className="card" key={ag.id}>
            <div className="btn-row" style={{ justifyContent: 'space-between' }}>
              <h3 style={{ margin: 0 }}>{ag.estabelecimentoNome}</h3>
              <StatusBadge status={ag.status} />
            </div>
            <p className="text-muted">
              {formatarData(ag.dataHora)} — {ag.quantidadePessoas} pessoa(s) — tolerância de {ag.toleranciaMinutos} min
            </p>
            {ag.status === 'PENDENTE' && (
              <p className="text-muted" style={{ fontSize: '0.82rem' }}>Aguardando confirmação do estabelecimento.</p>
            )}
            {ag.status === 'CANCELADO' && ag.motivoCancelamento && (
              <p className="text-muted" style={{ fontSize: '0.82rem' }}>Motivo: {ag.motivoCancelamento}</p>
            )}
            {STATUS_ATIVOS_AGENDAMENTO.includes(ag.status) && (
              <button
                className="danger small"
                onClick={() => setAgendamentoParaCancelar(ag)}
                disabled={cancelandoId === ag.id}
              >
                {cancelandoId === ag.id ? 'Cancelando...' : 'Cancelar'}
              </button>
            )}
            {podeAvaliar && (
              <button className="small" onClick={() => setAvaliandoItem(ag)}>Avaliar</button>
            )}
            {!podeAvaliar && concluido && !ag.avaliacaoLiberada && (
              <p className="text-muted" style={{ fontSize: '0.82rem' }}>
                Aguardando o estabelecimento liberar a mesa para avaliar.
              </p>
            )}
            {!podeAvaliar && concluido && ag.avaliacaoLiberada && (
              <p className="text-muted" style={{ fontSize: '0.82rem' }}>Avaliado.</p>
            )}
          </div>
        )
      })}

      {agendamentoParaCancelar && (
        <ConfirmModal
          titulo="Cancelar agendamento"
          mensagem={`Tem certeza que deseja cancelar o agendamento em ${agendamentoParaCancelar.estabelecimentoNome} (${formatarData(agendamentoParaCancelar.dataHora)})?`}
          textoConfirmar="Sim, cancelar"
          textoCancelar="Não"
          confirmando={cancelandoId === agendamentoParaCancelar.id}
          onConfirmar={cancelar}
          onCancelar={() => setAgendamentoParaCancelar(null)}
        />
      )}

      {avaliandoItem && (
        <AvaliacaoModal
          mensagem={`Como foi sua experiência em ${avaliandoItem.estabelecimentoNome}?`}
          confirmando={avaliando}
          onConfirmar={confirmarAvaliacao}
          onCancelar={() => setAvaliandoItem(null)}
        />
      )}
    </div>
  )
}
