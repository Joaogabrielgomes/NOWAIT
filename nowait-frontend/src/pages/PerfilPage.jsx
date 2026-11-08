import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { usuarioService, filaService, agendamentoService, avaliacaoService, extrairMensagemErro } from '../services/api'
import StatusBadge from '../components/StatusBadge'
import AvaliacaoModal from '../components/AvaliacaoModal'

const REGEX_EMAIL = /\S+@\S+\.\S+/
const REGEX_SENHA = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{6,}$/

function formatarDataHora(iso) {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

export default function PerfilPage() {
  const { usuario, atualizarUsuario, logout } = useAuth()
  const navigate = useNavigate()

  const [nome, setNome] = useState(usuario?.nome || '')
  const [email, setEmail] = useState(usuario?.email || '')
  const [senha, setSenha] = useState('')
  const [erros, setErros] = useState({})
  const [apiError, setApiError] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [sucesso, setSucesso] = useState(false)

  const [confirmandoExclusao, setConfirmandoExclusao] = useState(false)
  const [excluindo, setExcluindo] = useState(false)
  const [erroExclusao, setErroExclusao] = useState('')

  const [historico, setHistorico] = useState(null)
  const [pendentesAvaliacao, setPendentesAvaliacao] = useState({})
  const [avaliandoItem, setAvaliandoItem] = useState(null)
  const [avaliando, setAvaliando] = useState(false)
  const [erroAvaliacao, setErroAvaliacao] = useState('')

  const carregarHistorico = () => {
    Promise.all([filaService.meuHistorico(), agendamentoService.listarMeus(), avaliacaoService.listarPendentes()])
      .then(([fila, agendamentos, pendentes]) => {
        const itensFila = fila.data.map((f) => ({
          tipo: 'Fila',
          tipoAvaliacao: 'FILA',
          referenciaId: f.id,
          estabelecimentoNome: f.estabelecimentoNome,
          status: f.status,
          avaliacaoLiberada: f.avaliacaoLiberada,
          data: f.horaEntrada,
        }))
        const itensAgendamento = agendamentos.data.map((a) => ({
          tipo: 'Agendamento',
          tipoAvaliacao: 'AGENDAMENTO',
          referenciaId: a.id,
          estabelecimentoNome: a.estabelecimentoNome,
          status: a.status,
          avaliacaoLiberada: a.avaliacaoLiberada,
          data: a.dataHora,
        }))
        setHistorico(
          [...itensFila, ...itensAgendamento].sort((a, b) => new Date(b.data) - new Date(a.data))
        )

        const mapaPendentes = {}
        pendentes.data.forEach((p) => { mapaPendentes[`${p.tipo}-${p.referenciaId}`] = true })
        setPendentesAvaliacao(mapaPendentes)
      })
      .catch(() => setHistorico([]))
  }

  useEffect(() => {
    if (usuario?.role !== 'CLIENTE') return
    carregarHistorico()
  }, [usuario?.role])

  const confirmarAvaliacao = async (nota, comentario) => {
    setAvaliando(true)
    setErroAvaliacao('')
    try {
      const dto = avaliandoItem.tipoAvaliacao === 'FILA'
        ? { filaId: avaliandoItem.referenciaId, nota, comentario }
        : { agendamentoId: avaliandoItem.referenciaId, nota, comentario }
      await avaliacaoService.avaliar(dto)
      setAvaliandoItem(null)
      carregarHistorico()
    } catch (err) {
      setErroAvaliacao(extrairMensagemErro(err, 'Não foi possível enviar a avaliação.'))
    } finally {
      setAvaliando(false)
    }
  }

  function handleNomeChange(e) {
    setNome(e.target.value)
    setErros((p) => ({ ...p, nome: '' }))
    setApiError('')
  }

  function handleEmailChange(e) {
    setEmail(e.target.value)
    setErros((p) => ({ ...p, email: '' }))
    setApiError('')
  }

  function handleSenhaChange(e) {
    setSenha(e.target.value)
    setErros((p) => ({ ...p, senha: '' }))
    setApiError('')
  }

  function validar() {
    const e = {}
    if (!nome.trim()) e.nome = 'Nome obrigatório.'
    else if (nome.trim().length > 100) e.nome = 'Nome deve ter no máximo 100 caracteres.'

    if (!email.trim()) e.email = 'E-mail obrigatório.'
    else if (!REGEX_EMAIL.test(email)) e.email = 'Informe um e-mail válido.'

    if (senha && !REGEX_SENHA.test(senha)) {
      e.senha = 'Mín. 6 caracteres, 1 maiúscula, 1 minúscula e 1 caractere especial.'
    }
    return e
  }

  async function handleSalvar() {
    const e2 = validar()
    if (Object.keys(e2).length > 0) { setErros(e2); return }

    setSalvando(true)
    setApiError('')
    setSucesso(false)
    try {
      const { data } = await usuarioService.atualizarPerfil({
        nome: nome.trim(),
        email: email.trim(),
        senha: senha || undefined,
      })
      atualizarUsuario(data)
      setSenha('')
      setSucesso(true)
    } catch (err) {
      setApiError(extrairMensagemErro(err, 'Erro ao atualizar perfil.'))
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluirConta() {
    setExcluindo(true)
    setErroExclusao('')
    try {
      await usuarioService.excluirConta()
      logout()
      navigate('/')
    } catch (err) {
      setErroExclusao(extrairMensagemErro(err, 'Não foi possível excluir a conta.'))
      setExcluindo(false)
    }
  }

  return (
    <div className="container">
      <h2>Meu perfil</h2>

      <div className={usuario?.role === 'CLIENTE' ? 'grid-fit' : ''} style={{ alignItems: 'start' }}>
        <div className="card">
          {apiError && <div className="error-box">{apiError}</div>}
          {sucesso && <div className="info-box">Perfil atualizado com sucesso.</div>}

          <div className="form-group">
            <label>Nome</label>
            <input type="text" value={nome} onChange={handleNomeChange} maxLength={100} />
            {erros.nome && <span className="text-muted" style={{ color: 'var(--danger)', fontSize: '0.78rem' }}>{erros.nome}</span>}
          </div>

          <div className="form-group">
            <label>E-mail</label>
            <input type="email" value={email} onChange={handleEmailChange} />
            {erros.email && <span className="text-muted" style={{ color: 'var(--danger)', fontSize: '0.78rem' }}>{erros.email}</span>}
          </div>

          <div className="form-group">
            <label>Nova senha (deixe vazio para não alterar)</label>
            <input type="password" value={senha} onChange={handleSenhaChange} placeholder="••••••••" />
            {erros.senha && <span className="text-muted" style={{ color: 'var(--danger)', fontSize: '0.78rem' }}>{erros.senha}</span>}
          </div>

          <div className="btn-row">
            <button onClick={handleSalvar} disabled={salvando}>
              {salvando ? 'Salvando...' : 'Salvar alterações'}
            </button>
          </div>
        </div>

        {usuario?.role === 'CLIENTE' && (
          <div className="card">
            <h3 style={{ marginTop: 0 }}>Meu histórico</h3>
            {historico === null && <p className="text-muted">Carregando...</p>}
            {historico && historico.length === 0 && (
              <p className="text-muted">Você ainda não entrou em nenhuma fila nem fez agendamentos.</p>
            )}
            {erroAvaliacao && <div className="error-box">{erroAvaliacao}</div>}
            {historico && historico.length > 0 && (
              <div style={{ overflowX: 'auto' }}>
              <table>
                <thead><tr><th>Estabelecimento</th><th>Tipo</th><th>Status</th><th>Data</th><th></th></tr></thead>
                <tbody>
                  {historico.map((h, i) => {
                    const podeAvaliar = pendentesAvaliacao[`${h.tipoAvaliacao}-${h.referenciaId}`]
                    const emAndamento = h.status === 'ATENDIDO' || h.status === 'CONCLUIDO'
                    return (
                      <tr key={i}>
                        <td>{h.estabelecimentoNome}</td>
                        <td>{h.tipo}</td>
                        <td><StatusBadge status={h.status} /></td>
                        <td style={{ whiteSpace: 'nowrap' }}>{formatarDataHora(h.data)}</td>
                        <td style={{ minWidth: 110 }}>
                          {podeAvaliar && (
                            <button className="small" onClick={() => setAvaliandoItem(h)}>
                              Avaliar
                            </button>
                          )}
                          {!podeAvaliar && emAndamento && !h.avaliacaoLiberada && (
                            <span className="text-muted" style={{ fontSize: '0.75rem' }}>
                              Aguardando liberar mesa
                            </span>
                          )}
                          {!podeAvaliar && emAndamento && h.avaliacaoLiberada && (
                            <span className="text-muted" style={{ fontSize: '0.75rem' }}>Avaliado</span>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              </div>
            )}
          </div>
        )}
      </div>

      <div className="card mt-16" style={{ borderColor: 'var(--danger)' }}>
        <h3 style={{ color: 'var(--danger)', marginTop: 0 }}>Excluir conta</h3>
        <p className="text-muted" style={{ fontSize: '0.88rem', textAlign: 'justify' }}>
          Esta ação encerra o seu acesso ao NOWAIT. Caso você já tenha entradas de fila, agendamentos ou um
          estabelecimento vinculados, seus dados de identificação serão anonimizados; caso contrário, a conta
          será removida permanentemente.
        </p>

        {erroExclusao && <div className="error-box">{erroExclusao}</div>}

        {!confirmandoExclusao ? (
          <button className="danger small" onClick={() => setConfirmandoExclusao(true)}>
            Excluir minha conta
          </button>
        ) : (
          <div className="btn-row">
            <button className="danger small" onClick={handleExcluirConta} disabled={excluindo}>
              {excluindo ? 'Excluindo...' : 'Confirmar exclusão'}
            </button>
            <button className="secondary small" onClick={() => setConfirmandoExclusao(false)} disabled={excluindo}>
              Cancelar
            </button>
          </div>
        )}
      </div>

      {avaliandoItem && (
        <AvaliacaoModal
          titulo="Avaliar estabelecimento"
          mensagem={`Como foi sua experiência em ${avaliandoItem.estabelecimentoNome}?`}
          confirmando={avaliando}
          onConfirmar={confirmarAvaliacao}
          onCancelar={() => setAvaliandoItem(null)}
        />
      )}
    </div>
  )
}
