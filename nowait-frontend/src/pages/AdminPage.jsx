import { useEffect, useState } from 'react'
import { adminService, extrairMensagemErro } from '../services/api'
import StatusBadge from '../components/StatusBadge'
import GraficoAcoesPorDia, { formatarAcao } from '../components/GraficoAcoesPorDia'
import GraficoCrescimento from '../components/GraficoCrescimento'
import Paginacao from '../components/Paginacao'

const NOMES_PERFIL = { CLIENTE: 'Clientes', ESTABELECIMENTO: 'Estabelecimentos', ADMIN: 'Administradores' }

const ACOES_AUDITORIA = [
  'LOGIN_SUCESSO', 'LOGIN_FALHA', 'REGISTRO_CONTA', 'SENHA_RESET_SOLICITADO', 'SENHA_REDEFINIDA',
  'USUARIO_PERFIL_ATUALIZADO', 'USUARIO_CONTA_EXCLUIDA', 'USUARIO_CONTA_ANONIMIZADA',
  'FILA_ENTRADA', 'FILA_CHAMADA', 'FILA_CHAMADA_AUTOMATICA', 'FILA_ATENDIMENTO', 'FILA_CANCELAMENTO',
  'AGENDAMENTO_CRIADO', 'AGENDAMENTO_CANCELADO', 'AGENDAMENTO_CONFIRMADO', 'AGENDAMENTO_EXPIRADO',
  'MESA_CRIADA', 'MESA_STATUS_ALTERADO', 'MESA_LIBERADA', 'MESA_REMOVIDA', 'ADMIN_USUARIO_REMOVIDO',
]

const TAMANHO_PAGINA_AUDITORIA = 20

function formatarDataHora(iso) {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

function mascararEmail(email) {
  const arroba = email.indexOf('@')
  if (arroba <= 0) return email
  const local = email.slice(0, arroba)
  const dominio = email.slice(arroba + 1)

  const visivelLocal = local.slice(0, Math.min(2, local.length))
  const localMascarado = `${visivelLocal}${'*'.repeat(Math.max(3, local.length - visivelLocal.length))}`

  const ultimoPonto = dominio.lastIndexOf('.')
  const extensao = ultimoPonto >= 0 ? dominio.slice(ultimoPonto) : ''
  const dominioMascarado = `${'*'.repeat(Math.max(3, dominio.length - extensao.length))}${extensao}`

  return `${localMascarado}@${dominioMascarado}`
}

export default function AdminPage() {
  const [usuarios, setUsuarios] = useState([])
  const [estabelecimentos, setEstabelecimentos] = useState([])
  const [resumo, setResumo] = useState(null)
  const [carregandoResumo, setCarregandoResumo] = useState(true)
  const [aba, setAba] = useState('dashboard')
  const [erro, setErro] = useState('')

  const [filtroUsuarioId, setFiltroUsuarioId] = useState('')
  const [buscaUsuario, setBuscaUsuario] = useState('')
  const [filtroAcao, setFiltroAcao] = useState('')
  const [paginaAuditoria, setPaginaAuditoria] = useState(0)
  const [auditoria, setAuditoria] = useState(null)
  const [carregandoAuditoria, setCarregandoAuditoria] = useState(false)
  const [exportandoAuditoria, setExportandoAuditoria] = useState(false)

  const carregar = () => {
    adminService.listarUsuarios().then(({ data }) => setUsuarios(data))
    adminService.listarEstabelecimentos().then(({ data }) => setEstabelecimentos(data))
    setCarregandoResumo(true)
    adminService.dashboard()
      .then(({ data }) => setResumo(data))
      .finally(() => setCarregandoResumo(false))
  }

  useEffect(() => { carregar() }, [])

  const carregarAuditoria = (pagina = 0) => {
    setCarregandoAuditoria(true)
    adminService.auditoria({
      usuarioId: filtroUsuarioId || undefined,
      acao: filtroAcao || undefined,
      pagina,
      tamanho: TAMANHO_PAGINA_AUDITORIA,
    })
      .then(({ data }) => { setAuditoria(data); setPaginaAuditoria(pagina) })
      .finally(() => setCarregandoAuditoria(false))
  }

  useEffect(() => {
    if (aba === 'auditoria' && auditoria === null) carregarAuditoria(0)
  }, [aba])

  const aplicarFiltroAuditoria = (e) => {
    e.preventDefault()
    carregarAuditoria(0)
  }

  const exportarAuditoriaCsv = async () => {
    setExportandoAuditoria(true)
    try {
      const { data } = await adminService.exportarAuditoria({
        usuarioId: filtroUsuarioId || undefined,
        acao: filtroAcao || undefined,
      })
      const url = URL.createObjectURL(new Blob([data], { type: 'text/csv;charset=utf-8' }))
      const link = document.createElement('a')
      link.href = url
      link.download = 'auditoria.csv'
      link.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível exportar a auditoria'))
    } finally {
      setExportandoAuditoria(false)
    }
  }

  const removerUsuario = async (id) => {
    setErro('')
    try {
      await adminService.removerUsuario(id)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível remover o usuário'))
    }
  }

  return (
    <div className="container">
      <h2>Administração</h2>
      {erro && <div className="error-box">{erro}</div>}

      <div className="btn-row mb-16">
        <button className={aba === 'dashboard' ? '' : 'secondary'} onClick={() => setAba('dashboard')}>
          Dashboard
        </button>
        <button className={aba === 'usuarios' ? '' : 'secondary'} onClick={() => setAba('usuarios')}>
          Usuários ({usuarios.length})
        </button>
        <button className={aba === 'estabelecimentos' ? '' : 'secondary'} onClick={() => setAba('estabelecimentos')}>
          Estabelecimentos ({estabelecimentos.length})
        </button>
        <button className={aba === 'auditoria' ? '' : 'secondary'} onClick={() => setAba('auditoria')}>
          Auditoria
        </button>
      </div>

      {aba === 'dashboard' && (
        <>
          {carregandoResumo && <p className="text-muted">Carregando...</p>}

          {!carregandoResumo && resumo && (
            <>
              <div className="grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', marginBottom: 24 }}>
                {Object.entries(resumo.usuariosPorPerfil).map(([perfil, total]) => (
                  <div className="card" key={perfil} style={{ marginBottom: 0, textAlign: 'center' }}>
                    <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--primary)' }}>{total}</div>
                    <p className="text-muted" style={{ margin: 0 }}>{NOMES_PERFIL[perfil] || perfil}</p>
                  </div>
                ))}

                <div className="card" style={{ marginBottom: 0, textAlign: 'center' }}>
                  <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--success)' }}>
                    {resumo.estabelecimentosAbertos}
                  </div>
                  <p className="text-muted" style={{ margin: 0 }}>
                    Estabelecimentos abertos ({resumo.estabelecimentosFechados} fechados)
                  </p>
                </div>

                <div className="card" style={{ marginBottom: 0, textAlign: 'center' }}>
                  <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--warning)' }}>
                    {resumo.pessoasNaFilaAgora}
                  </div>
                  <p className="text-muted" style={{ margin: 0 }}>Pessoas na fila agora</p>
                </div>
              </div>

              <div className="card">
                <h3 style={{ marginTop: 0 }}>Novos cadastros por dia</h3>
                <GraficoCrescimento dados={resumo.crescimentoPorDia} />
              </div>

              <div className="card">
                <h3 style={{ marginTop: 0 }}>Ações de auditoria por dia</h3>
                <GraficoAcoesPorDia dados={resumo.acoesPorDia} />
              </div>

              <div className="card">
                <h3 style={{ marginTop: 0 }}>Atividade recente</h3>
                {resumo.atividadeRecente.length === 0 && (
                  <p className="text-muted">Nenhuma ação registrada ainda.</p>
                )}
                {resumo.atividadeRecente.length > 0 && (
                  <table>
                    <thead><tr><th>Data</th><th>Ação</th><th>Entidade</th><th>Detalhes</th></tr></thead>
                    <tbody>
                      {resumo.atividadeRecente.map((log) => (
                        <tr key={log.id}>
                          <td style={{ whiteSpace: 'nowrap' }}>{formatarDataHora(log.criadoEm)}</td>
                          <td><span className="badge" style={{ background: 'var(--bg-elevated)', color: 'var(--text)' }}>{formatarAcao(log.acao)}</span></td>
                          <td className="text-muted">{log.entidadeTipo || '—'}</td>
                          <td className="text-muted">{log.detalhes || '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </>
          )}
        </>
      )}

      {aba === 'usuarios' && (
        <table>
          <thead><tr><th>Nome</th><th>Email</th><th>Perfil</th><th></th></tr></thead>
          <tbody>
            {usuarios.map((u) => (
              <tr key={u.id}>
                <td>{u.nome}</td>
                <td>{mascararEmail(u.email)}</td>
                <td><StatusBadge status={u.role} /></td>
                <td>
                  {u.role !== 'ADMIN' && (
                    <button className="danger small" onClick={() => removerUsuario(u.id)}>Remover</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {aba === 'estabelecimentos' && (
        <table>
          <thead><tr><th>Nome</th><th>Cidade</th><th>Status</th></tr></thead>
          <tbody>
            {estabelecimentos.map((e) => (
              <tr key={e.id}>
                <td>{e.nome}</td>
                <td>{e.endereco.cidade}/{e.endereco.estado}</td>
                <td><span className={`badge ${e.aberto ? 'CHAMADO' : 'CANCELADO'}`}>{e.aberto ? 'Aberto' : 'Fechado'}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {aba === 'auditoria' && (
        <div className="card">
          <form onSubmit={aplicarFiltroAuditoria} className="form-row" style={{ alignItems: 'flex-end' }}>
            <div className="form-group">
              <label>Usuário</label>
              <input
                list="usuarios-datalist"
                placeholder="Todos (digite o nome para buscar)"
                value={buscaUsuario}
                onChange={(e) => {
                  const texto = e.target.value
                  setBuscaUsuario(texto)
                  const encontrado = usuarios.find((u) => `${u.nome} (${mascararEmail(u.email)})` === texto)
                  setFiltroUsuarioId(encontrado ? encontrado.id : '')
                }}
              />
              <datalist id="usuarios-datalist">
                {usuarios.map((u) => (
                  <option key={u.id} value={`${u.nome} (${mascararEmail(u.email)})`} />
                ))}
              </datalist>
            </div>
            <div className="form-group">
              <label>Ação</label>
              <select value={filtroAcao} onChange={(e) => setFiltroAcao(e.target.value)}>
                <option value="">Todas</option>
                {ACOES_AUDITORIA.map((a) => (
                  <option key={a} value={a}>{formatarAcao(a)}</option>
                ))}
              </select>
            </div>
            <div className="form-group" style={{ flex: 'none' }}>
              <button type="submit" disabled={carregandoAuditoria}>Filtrar</button>
            </div>
            <div className="form-group" style={{ flex: 'none' }}>
              <button
                type="button"
                className="secondary"
                onClick={exportarAuditoriaCsv}
                disabled={exportandoAuditoria}
              >
                {exportandoAuditoria ? 'Exportando...' : 'Exportar CSV'}
              </button>
            </div>
          </form>

          {carregandoAuditoria && <p className="text-muted">Carregando...</p>}

          {!carregandoAuditoria && auditoria && (
            <>
              {auditoria.content.length === 0 ? (
                <p className="text-muted">Nenhum registro encontrado para esse filtro.</p>
              ) : (
                <table>
                  <thead><tr><th>Data</th><th>Ação</th><th>Entidade</th><th>Detalhes</th></tr></thead>
                  <tbody>
                    {auditoria.content.map((log) => (
                      <tr key={log.id}>
                        <td style={{ whiteSpace: 'nowrap' }}>{formatarDataHora(log.criadoEm)}</td>
                        <td><span className="badge" style={{ background: 'var(--bg-elevated)', color: 'var(--text)' }}>{formatarAcao(log.acao)}</span></td>
                        <td className="text-muted">{log.entidadeTipo || '—'}</td>
                        <td className="text-muted">{log.detalhes || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              <Paginacao
                paginaAtual={paginaAuditoria + 1}
                totalPaginas={auditoria.totalPages}
                aoMudar={(p) => carregarAuditoria(p - 1)}
                ariaLabel="Paginação de auditoria"
              />
            </>
          )}
        </div>
      )}
    </div>
  )
}
