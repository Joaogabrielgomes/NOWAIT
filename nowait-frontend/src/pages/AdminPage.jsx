import { useEffect, useState } from 'react'
import { adminService, extrairMensagemErro } from '../services/api'
import StatusBadge from '../components/StatusBadge'
import GraficoAcoesPorDia, { formatarAcao, formatarEntidade } from '../components/GraficoAcoesPorDia'
import GraficoCrescimento from '../components/GraficoCrescimento'
import Paginacao from '../components/Paginacao'
import DenunciaResolverModal from '../components/DenunciaResolverModal'
import MotivoModal from '../components/MotivoModal'
import { rotuloCategoria } from '../constants/categorias'

const NOMES_PERFIL = { CLIENTE: 'Clientes', ESTABELECIMENTO: 'Estabelecimentos', ADMIN: 'Administradores' }

const ACOES_AUDITORIA = [
  'LOGIN_SUCESSO', 'LOGIN_FALHA', 'REGISTRO_CONTA', 'SENHA_RESET_SOLICITADO', 'SENHA_REDEFINIDA',
  'USUARIO_PERFIL_ATUALIZADO', 'USUARIO_CONTA_EXCLUIDA', 'USUARIO_CONTA_ANONIMIZADA',
  'FILA_ENTRADA', 'FILA_CHAMADA', 'FILA_CHAMADA_AUTOMATICA', 'FILA_ATENDIMENTO', 'FILA_CANCELAMENTO',
  'AGENDAMENTO_CRIADO', 'AGENDAMENTO_CANCELADO', 'AGENDAMENTO_CONFIRMADO', 'AGENDAMENTO_EXPIRADO',
  'MESA_CRIADA', 'MESA_STATUS_ALTERADO', 'MESA_LIBERADA', 'MESA_REMOVIDA', 'ADMIN_USUARIO_REMOVIDO',
  'ADMIN_USUARIO_BANIDO', 'ADMIN_USUARIO_DESBANIDO',
  'ADMIN_ESTABELECIMENTO_APROVADO', 'ADMIN_ESTABELECIMENTO_REJEITADO',
  'DENUNCIA_CRIADA', 'DENUNCIA_RESOLVIDA',
  'CONTESTACAO_BANIMENTO_CRIADA', 'CONTESTACAO_BANIMENTO_ANALISADA',
]

const OPCOES_ORDENACAO = [
  { valor: 'nome-asc', campo: 'nome', ordem: 'asc', rotulo: 'Nome (A → Z)' },
  { valor: 'nome-desc', campo: 'nome', ordem: 'desc', rotulo: 'Nome (Z → A)' },
  { valor: 'createdAt-desc', campo: 'createdAt', ordem: 'desc', rotulo: 'Mais recentes primeiro' },
  { valor: 'createdAt-asc', campo: 'createdAt', ordem: 'asc', rotulo: 'Mais antigos primeiro' },
]

const ROTULOS_STATUS_APROVACAO = { PENDENTE: 'Pendente', APROVADO: 'Aprovado', REJEITADO: 'Rejeitado' }
const ROTULOS_STATUS_DENUNCIA = { PENDENTE: 'Pendente', APROVADA: 'Aprovada', REJEITADA: 'Rejeitada' }
const ROTULOS_TIPO_ALVO_DENUNCIA = { ESTABELECIMENTO: 'Estabelecimento', AVALIACAO: 'Avaliação', USUARIO: 'Usuário' }

const TAMANHO_PAGINA_AUDITORIA = 20
const TAMANHO_PAGINA_USUARIOS = 20
const TAMANHO_PAGINA_ESTABELECIMENTOS = 10
const TAMANHO_PAGINA_DENUNCIAS = 10
const TAMANHO_PAGINA_CONTESTACOES = 10

const TAMANHO_SUGESTOES_USUARIO = 8

const DEBOUNCE_BUSCA_MS = 350

const PAGINA_VAZIA = { content: [], totalPages: 0, totalElements: 0 }

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
  const [resumo, setResumo] = useState(null)
  const [carregandoResumo, setCarregandoResumo] = useState(true)
  const [aba, setAba] = useState('dashboard')
  const [erro, setErro] = useState('')

  const [usuariosPage, setUsuariosPage] = useState(PAGINA_VAZIA)
  const [paginaUsuarios, setPaginaUsuarios] = useState(0)
  const [filtroRoleUsuario, setFiltroRoleUsuario] = useState('')
  const [buscaNomeUsuario, setBuscaNomeUsuario] = useState('')
  const [filtroNomeUsuario, setFiltroNomeUsuario] = useState('')
  const [ordenacaoUsuario, setOrdenacaoUsuario] = useState('nome-asc')
  const [carregandoUsuarios, setCarregandoUsuarios] = useState(true)

  const [estabelecimentosPage, setEstabelecimentosPage] = useState(PAGINA_VAZIA)
  const [paginaEstabelecimentos, setPaginaEstabelecimentos] = useState(0)
  const [filtroStatusEstabelecimento, setFiltroStatusEstabelecimento] = useState('')
  const [buscaNomeEstabelecimento, setBuscaNomeEstabelecimento] = useState('')
  const [filtroNomeEstabelecimento, setFiltroNomeEstabelecimento] = useState('')
  const [ordenacaoEstabelecimento, setOrdenacaoEstabelecimento] = useState('nome-asc')
  const [carregandoEstabelecimentos, setCarregandoEstabelecimentos] = useState(true)

  const [denunciasPage, setDenunciasPage] = useState(PAGINA_VAZIA)
  const [paginaDenuncias, setPaginaDenuncias] = useState(0)
  const [filtroStatusDenuncia, setFiltroStatusDenuncia] = useState('PENDENTE')
  const [buscaNomeDenuncia, setBuscaNomeDenuncia] = useState('')
  const [filtroNomeDenuncia, setFiltroNomeDenuncia] = useState('')
  const [carregandoDenuncias, setCarregandoDenuncias] = useState(false)
  const [denunciaResolvendo, setDenunciaResolvendo] = useState(null)
  const [enviandoResolucao, setEnviandoResolucao] = useState(false)

  const [usuarioBanindo, setUsuarioBanindo] = useState(null)
  const [banindo, setBanindo] = useState(false)

  const [estabelecimentoRejeitando, setEstabelecimentoRejeitando] = useState(null)
  const [rejeitandoEstabelecimento, setRejeitandoEstabelecimento] = useState(false)

  const [contestacoesPage, setContestacoesPage] = useState(PAGINA_VAZIA)
  const [paginaContestacoes, setPaginaContestacoes] = useState(0)
  const [filtroStatusContestacao, setFiltroStatusContestacao] = useState('PENDENTE')
  const [buscaNomeContestacao, setBuscaNomeContestacao] = useState('')
  const [filtroNomeContestacao, setFiltroNomeContestacao] = useState('')
  const [carregandoContestacoes, setCarregandoContestacoes] = useState(false)

  const [filtroUsuarioId, setFiltroUsuarioId] = useState('')
  const [buscaUsuario, setBuscaUsuario] = useState('')
  const [sugestoesUsuario, setSugestoesUsuario] = useState([])
  const [mostrarSugestoesUsuario, setMostrarSugestoesUsuario] = useState(false)
  const [filtroAcao, setFiltroAcao] = useState('')
  const [paginaAuditoria, setPaginaAuditoria] = useState(0)
  const [auditoria, setAuditoria] = useState(null)
  const [carregandoAuditoria, setCarregandoAuditoria] = useState(false)
  const [exportandoAuditoria, setExportandoAuditoria] = useState(false)

  const carregarUsuarios = (pagina = 0) => {
    setCarregandoUsuarios(true)
    const { campo, ordem } = OPCOES_ORDENACAO.find((o) => o.valor === ordenacaoUsuario)
    adminService.listarUsuarios({
      role: filtroRoleUsuario || undefined,
      nome: filtroNomeUsuario || undefined,
      campo,
      ordem,
      pagina,
      tamanho: TAMANHO_PAGINA_USUARIOS,
    })
      .then(({ data }) => { setUsuariosPage(data); setPaginaUsuarios(pagina) })
      .finally(() => setCarregandoUsuarios(false))
  }

  const carregarEstabelecimentos = (pagina = 0) => {
    setCarregandoEstabelecimentos(true)
    const { campo, ordem } = OPCOES_ORDENACAO.find((o) => o.valor === ordenacaoEstabelecimento)
    adminService.listarEstabelecimentos({
      statusAprovacao: filtroStatusEstabelecimento || undefined,
      nome: filtroNomeEstabelecimento || undefined,
      campo,
      ordem,
      pagina,
      tamanho: TAMANHO_PAGINA_ESTABELECIMENTOS,
    })
      .then(({ data }) => { setEstabelecimentosPage(data); setPaginaEstabelecimentos(pagina) })
      .finally(() => setCarregandoEstabelecimentos(false))
  }

  const carregarResumo = () => {
    setCarregandoResumo(true)
    adminService.dashboard()
      .then(({ data }) => setResumo(data))
      .finally(() => setCarregandoResumo(false))
  }

  const carregarDenuncias = (pagina = 0) => {
    setCarregandoDenuncias(true)
    adminService.listarDenuncias({
      status: filtroStatusDenuncia || undefined,
      nome: filtroNomeDenuncia || undefined,
      pagina,
      tamanho: TAMANHO_PAGINA_DENUNCIAS,
    })
      .then(({ data }) => { setDenunciasPage(data); setPaginaDenuncias(pagina) })
      .finally(() => setCarregandoDenuncias(false))
  }

  const carregarContestacoes = (pagina = 0) => {
    setCarregandoContestacoes(true)
    adminService.listarContestacoes({
      status: filtroStatusContestacao || undefined,
      nome: filtroNomeContestacao || undefined,
      pagina,
      tamanho: TAMANHO_PAGINA_CONTESTACOES,
    })
      .then(({ data }) => { setContestacoesPage(data); setPaginaContestacoes(pagina) })
      .finally(() => setCarregandoContestacoes(false))
  }

  const carregar = () => {
    carregarUsuarios(paginaUsuarios)
    carregarEstabelecimentos(paginaEstabelecimentos)
    carregarDenuncias(paginaDenuncias)
    carregarContestacoes(paginaContestacoes)
    carregarResumo()
  }

  useEffect(() => { carregarResumo() }, [])
  useEffect(() => { carregarUsuarios(0) }, [filtroRoleUsuario, filtroNomeUsuario, ordenacaoUsuario])
  useEffect(() => { carregarEstabelecimentos(0) }, [filtroStatusEstabelecimento, filtroNomeEstabelecimento, ordenacaoEstabelecimento])
  useEffect(() => { carregarDenuncias(0) }, [filtroStatusDenuncia, filtroNomeDenuncia])
  useEffect(() => { carregarContestacoes(0) }, [filtroStatusContestacao, filtroNomeContestacao])

  useEffect(() => {
    const timer = setTimeout(() => setFiltroNomeUsuario(buscaNomeUsuario), DEBOUNCE_BUSCA_MS)
    return () => clearTimeout(timer)
  }, [buscaNomeUsuario])
  useEffect(() => {
    const timer = setTimeout(() => setFiltroNomeEstabelecimento(buscaNomeEstabelecimento), DEBOUNCE_BUSCA_MS)
    return () => clearTimeout(timer)
  }, [buscaNomeEstabelecimento])
  useEffect(() => {
    const timer = setTimeout(() => setFiltroNomeDenuncia(buscaNomeDenuncia), DEBOUNCE_BUSCA_MS)
    return () => clearTimeout(timer)
  }, [buscaNomeDenuncia])
  useEffect(() => {
    const timer = setTimeout(() => setFiltroNomeContestacao(buscaNomeContestacao), DEBOUNCE_BUSCA_MS)
    return () => clearTimeout(timer)
  }, [buscaNomeContestacao])

  useEffect(() => {
    const texto = buscaUsuario.trim()
    if (texto.length === 0) {
      setSugestoesUsuario([])
      return
    }
    const timer = setTimeout(() => {
      adminService.listarUsuarios({ nome: texto, tamanho: TAMANHO_SUGESTOES_USUARIO })
        .then(({ data }) => setSugestoesUsuario(data.content))
    }, DEBOUNCE_BUSCA_MS)
    return () => clearTimeout(timer)
  }, [buscaUsuario])

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

  const digitarBuscaUsuario = (texto) => {
    setBuscaUsuario(texto)
    setFiltroUsuarioId('')
    setMostrarSugestoesUsuario(true)
  }

  const selecionarSugestaoUsuario = (u) => {
    setBuscaUsuario(`${u.nome} (${mascararEmail(u.email)})`)
    setFiltroUsuarioId(u.id)
    setSugestoesUsuario([])
    setMostrarSugestoesUsuario(false)
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

  const confirmarBanimento = async (motivo) => {
    setErro('')
    setBanindo(true)
    try {
      await adminService.banirUsuario(usuarioBanindo.id, motivo)
      setUsuarioBanindo(null)
      carregarUsuarios(paginaUsuarios)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível banir o usuário'))
    } finally {
      setBanindo(false)
    }
  }

  const desbanirUsuario = async (id) => {
    setErro('')
    try {
      await adminService.desbanirUsuario(id)
      carregarUsuarios(paginaUsuarios)
      carregarContestacoes(paginaContestacoes)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível desbanir o usuário'))
    }
  }

  const desbanirPelaContestacao = async (contestacao) => {
    await desbanirUsuario(contestacao.alvoId)
    await marcarContestacaoAnalisada(contestacao.id)
  }

  const marcarContestacaoAnalisada = async (id) => {
    setErro('')
    try {
      await adminService.marcarContestacaoAnalisada(id)
      carregarContestacoes(paginaContestacoes)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível marcar a contestação como analisada'))
    }
  }

  const marcarContestacaoRejeicaoEstabelecimentoAnalisada = async (id) => {
    setErro('')
    try {
      await adminService.marcarContestacaoRejeicaoEstabelecimentoAnalisada(id)
      carregarContestacoes(paginaContestacoes)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível marcar a contestação como analisada'))
    }
  }

  const reaprovarPelaContestacao = async (contestacao) => {
    await aprovarEstabelecimento(contestacao.alvoId)
    await marcarContestacaoRejeicaoEstabelecimentoAnalisada(contestacao.id)
  }

  const resolverDenuncia = async (dto) => {
    setErro('')
    setEnviandoResolucao(true)
    try {
      await adminService.resolverDenuncia(denunciaResolvendo.id, dto)
      setDenunciaResolvendo(null)
      carregarDenuncias(paginaDenuncias)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível resolver a denúncia'))
    } finally {
      setEnviandoResolucao(false)
    }
  }

  const aprovarEstabelecimento = async (id) => {
    setErro('')
    try {
      await adminService.aprovarEstabelecimento(id)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível aprovar o estabelecimento'))
    }
  }

  const confirmarRejeicaoEstabelecimento = async (motivo) => {
    setErro('')
    setRejeitandoEstabelecimento(true)
    try {
      await adminService.rejeitarEstabelecimento(estabelecimentoRejeitando.id, motivo)
      setEstabelecimentoRejeitando(null)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível rejeitar o estabelecimento'))
    } finally {
      setRejeitandoEstabelecimento(false)
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
          Usuários ({usuariosPage.totalElements})
        </button>
        <button className={aba === 'estabelecimentos' ? '' : 'secondary'} onClick={() => setAba('estabelecimentos')}>
          Estabelecimentos ({estabelecimentosPage.totalElements})
        </button>
        <button className={aba === 'denuncias' ? '' : 'secondary'} onClick={() => setAba('denuncias')}>
          Denúncias ({denunciasPage.totalElements})
        </button>
        <button className={aba === 'contestacoes' ? '' : 'secondary'} onClick={() => setAba('contestacoes')}>
          Contestações ({contestacoesPage.totalElements})
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
                    <thead><tr><th>Data</th><th>Usuário</th><th>Ação</th><th>Entidade</th><th>Detalhes</th></tr></thead>
                    <tbody>
                      {resumo.atividadeRecente.map((log) => (
                        <tr key={log.id}>
                          <td style={{ whiteSpace: 'nowrap' }}>{formatarDataHora(log.criadoEm)}</td>
                          <td>{log.usuarioNome || '—'}</td>
                          <td>{formatarAcao(log.acao)}</td>
                          <td className="text-muted">{formatarEntidade(log.entidadeTipo)}</td>
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
        <div>
          <div className="form-row mb-16" style={{ maxWidth: 640 }}>
            <div className="form-group">
              <label>Buscar por nome</label>
              <input
                type="text"
                value={buscaNomeUsuario}
                onChange={(e) => setBuscaNomeUsuario(e.target.value)}
                placeholder="Nome do usuário..."
              />
            </div>
            <div className="form-group">
              <label>Tipo de usuário</label>
              <select value={filtroRoleUsuario} onChange={(e) => setFiltroRoleUsuario(e.target.value)}>
                <option value="">Todos</option>
                <option value="CLIENTE">Clientes</option>
                <option value="ESTABELECIMENTO">Estabelecimentos</option>
                <option value="ADMIN">Administradores</option>
              </select>
            </div>
            <div className="form-group">
              <label>Ordenar por</label>
              <select value={ordenacaoUsuario} onChange={(e) => setOrdenacaoUsuario(e.target.value)}>
                {OPCOES_ORDENACAO.map((o) => (
                  <option key={o.valor} value={o.valor}>{o.rotulo}</option>
                ))}
              </select>
            </div>
          </div>

          {carregandoUsuarios && <p className="text-muted">Carregando...</p>}

          {!carregandoUsuarios && (
            <>
              {usuariosPage.content.length === 0 ? (
                <p className="text-muted">Nenhum usuário encontrado para esse filtro.</p>
              ) : (
                <table>
                  <thead><tr><th>Nome</th><th>Email</th><th>Perfil</th><th>Status</th><th></th></tr></thead>
                  <tbody>
                    {usuariosPage.content.map((u) => (
                      <tr key={u.id}>
                        <td>{u.nome}</td>
                        <td>{mascararEmail(u.email)}</td>
                        <td><StatusBadge status={u.role} /></td>
                        <td>
                          {u.banido
                            ? <span className="badge CANCELADO">Banido</span>
                            : <span className="text-muted">—</span>}
                        </td>
                        <td>
                          {u.role !== 'ADMIN' && (
                            <div className="btn-row">
                              {u.banido ? (
                                <button className="small" onClick={() => desbanirUsuario(u.id)}>Desbanir</button>
                              ) : (
                                <button className="secondary small" onClick={() => setUsuarioBanindo(u)}>Banir</button>
                              )}
                              <button className="danger small" onClick={() => removerUsuario(u.id)}>Remover</button>
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              <Paginacao
                paginaAtual={paginaUsuarios + 1}
                totalPaginas={usuariosPage.totalPages}
                aoMudar={(p) => carregarUsuarios(p - 1)}
                ariaLabel="Paginação de usuários"
              />
            </>
          )}
        </div>
      )}

      {aba === 'estabelecimentos' && (
        <div>
          <div className="form-row mb-16" style={{ maxWidth: 640 }}>
            <div className="form-group">
              <label>Buscar por nome</label>
              <input
                type="text"
                value={buscaNomeEstabelecimento}
                onChange={(e) => setBuscaNomeEstabelecimento(e.target.value)}
                placeholder="Nome do estabelecimento..."
              />
            </div>
            <div className="form-group">
              <label>Status de aprovação</label>
              <select value={filtroStatusEstabelecimento} onChange={(e) => setFiltroStatusEstabelecimento(e.target.value)}>
                <option value="">Todos</option>
                <option value="APROVADO">Aprovados</option>
                <option value="REJEITADO">Reprovados</option>
                <option value="PENDENTE">Pendentes</option>
              </select>
            </div>
            <div className="form-group">
              <label>Ordenar por</label>
              <select value={ordenacaoEstabelecimento} onChange={(e) => setOrdenacaoEstabelecimento(e.target.value)}>
                {OPCOES_ORDENACAO.map((o) => (
                  <option key={o.valor} value={o.valor}>{o.rotulo}</option>
                ))}
              </select>
            </div>
          </div>

          {carregandoEstabelecimentos && <p className="text-muted">Carregando...</p>}

          {!carregandoEstabelecimentos && (
            <>
              {estabelecimentosPage.content.length === 0 && (
                <p className="text-muted">Nenhum estabelecimento encontrado para esse filtro.</p>
              )}

              {estabelecimentosPage.content.map((e) => (
                <div className="card" key={e.id}>
                  <div className="btn-row" style={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                      <h3 style={{ marginBottom: 4 }}>{e.nome}</h3>
                      <p className="text-muted" style={{ margin: 0 }}>{rotuloCategoria(e.categoria)}</p>
                    </div>
                    <div className="btn-row">
                      <span className={`badge ${e.aberto ? 'CHAMADO' : 'CANCELADO'}`}>{e.aberto ? 'Aberto' : 'Fechado'}</span>
                      <span className={`badge ${e.statusAprovacao === 'APROVADO' ? 'CHAMADO' : e.statusAprovacao === 'REJEITADO' ? 'CANCELADO' : 'AGUARDANDO'}`}>
                        {ROTULOS_STATUS_APROVACAO[e.statusAprovacao] || e.statusAprovacao}
                      </span>
                    </div>
                  </div>

                  <p className="text-muted" style={{ marginBottom: 4 }}>
                    {e.endereco.logradouro}, {e.endereco.numero}
                    {e.endereco.complemento ? ` — ${e.endereco.complemento}` : ''}
                    {' — '}{e.endereco.bairro}, {e.endereco.cidade}/{e.endereco.estado} — CEP {e.endereco.cep}
                  </p>
                  <p className="text-muted" style={{ marginBottom: 0 }}>
                    {e.telefone ? `Telefone: ${e.telefone}` : 'Telefone não informado'}
                    {' · '}Funcionamento: {e.horarioAbertura?.slice(0, 5)} às {e.horarioFechamento?.slice(0, 5)}
                  </p>

                  {e.statusAprovacao === 'PENDENTE' && (
                    <div className="btn-row mt-16">
                      <button className="small" onClick={() => aprovarEstabelecimento(e.id)}>Aprovar</button>
                      <button className="danger small" onClick={() => setEstabelecimentoRejeitando(e)}>Rejeitar</button>
                    </div>
                  )}
                </div>
              ))}

              <Paginacao
                paginaAtual={paginaEstabelecimentos + 1}
                totalPaginas={estabelecimentosPage.totalPages}
                aoMudar={(p) => carregarEstabelecimentos(p - 1)}
                ariaLabel="Paginação de estabelecimentos"
              />
            </>
          )}
        </div>
      )}

      {aba === 'denuncias' && (
        <div>
          <div className="form-row mb-16" style={{ maxWidth: 480 }}>
            <div className="form-group">
              <label>Buscar por nome do denunciante</label>
              <input
                type="text"
                value={buscaNomeDenuncia}
                onChange={(e) => setBuscaNomeDenuncia(e.target.value)}
                placeholder="Nome de quem denunciou..."
              />
            </div>
            <div className="form-group" style={{ flex: 'none', minWidth: 160 }}>
              <label>Status</label>
              <select value={filtroStatusDenuncia} onChange={(e) => setFiltroStatusDenuncia(e.target.value)}>
                <option value="">Todas</option>
                <option value="PENDENTE">Pendentes</option>
                <option value="APROVADA">Aprovadas</option>
                <option value="REJEITADA">Rejeitadas</option>
              </select>
            </div>
          </div>

          {carregandoDenuncias && <p className="text-muted">Carregando...</p>}

          {!carregandoDenuncias && (
            <>
              {denunciasPage.content.length === 0 && (
                <p className="text-muted">Nenhuma denúncia encontrada para esse filtro.</p>
              )}

              {denunciasPage.content.map((d) => (
                <div className="card" key={d.id}>
                  <div className="btn-row" style={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                      <h3 style={{ marginBottom: 4 }}>
                        {ROTULOS_TIPO_ALVO_DENUNCIA[d.tipoAlvo] || d.tipoAlvo}: {d.alvoDescricao}
                      </h3>
                      <p className="text-muted" style={{ margin: 0 }}>Denunciado por {d.denuncianteNome}</p>
                    </div>
                    <span className={`badge ${d.status === 'APROVADA' ? 'CHAMADO' : d.status === 'REJEITADA' ? 'CANCELADO' : 'AGUARDANDO'}`}>
                      {ROTULOS_STATUS_DENUNCIA[d.status] || d.status}
                    </span>
                  </div>

                  <p style={{ marginBottom: 4 }}>{d.motivo}</p>
                  <p className="text-muted" style={{ marginBottom: 0 }}>{formatarDataHora(d.createdAt)}</p>

                  {d.observacaoAdmin && (
                    <p className="text-muted" style={{ marginBottom: 0 }}>
                      Observação do admin: {d.observacaoAdmin}
                    </p>
                  )}

                  {d.status === 'PENDENTE' && (
                    <div className="btn-row mt-16">
                      <button className="small" onClick={() => setDenunciaResolvendo(d)}>Resolver denúncia</button>
                    </div>
                  )}
                </div>
              ))}

              <Paginacao
                paginaAtual={paginaDenuncias + 1}
                totalPaginas={denunciasPage.totalPages}
                aoMudar={(p) => carregarDenuncias(p - 1)}
                ariaLabel="Paginação de denúncias"
              />
            </>
          )}

          {denunciaResolvendo && (
            <DenunciaResolverModal
              denuncia={denunciaResolvendo}
              resolvendo={enviandoResolucao}
              onResolver={resolverDenuncia}
              onCancelar={() => setDenunciaResolvendo(null)}
            />
          )}
        </div>
      )}

      {aba === 'contestacoes' && (
        <div>
          <div className="form-row mb-16" style={{ maxWidth: 480 }}>
            <div className="form-group">
              <label>Buscar por nome</label>
              <input
                type="text"
                value={buscaNomeContestacao}
                onChange={(e) => setBuscaNomeContestacao(e.target.value)}
                placeholder="Nome do usuário ou estabelecimento..."
              />
            </div>
            <div className="form-group" style={{ flex: 'none', minWidth: 160 }}>
              <label>Status</label>
              <select value={filtroStatusContestacao} onChange={(e) => setFiltroStatusContestacao(e.target.value)}>
                <option value="">Todas</option>
                <option value="PENDENTE">Pendentes</option>
                <option value="ANALISADA">Analisadas</option>
              </select>
            </div>
          </div>

          {carregandoContestacoes && <p className="text-muted">Carregando...</p>}

          {!carregandoContestacoes && (
            <>
              {contestacoesPage.content.length === 0 && (
                <p className="text-muted">Nenhuma contestação encontrada para esse filtro.</p>
              )}

              {contestacoesPage.content.map((c) => {
                const ehBanimento = c.tipo === 'BANIMENTO'
                return (
                  <div className="card" key={c.id}>
                    <div className="btn-row" style={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <div>
                        <h3 style={{ marginBottom: 4 }}>{c.nomeAlvo}</h3>
                        <p className="text-muted" style={{ margin: 0 }}>{mascararEmail(c.emailAlvo)}</p>
                      </div>
                      <div className="btn-row">
                        <span className="badge AGUARDANDO">{ehBanimento ? 'Banimento' : 'Rejeição de estabelecimento'}</span>
                        <span className={`badge ${c.status === 'ANALISADA' ? 'CHAMADO' : 'AGUARDANDO'}`}>
                          {c.status === 'ANALISADA' ? 'Analisada' : 'Pendente'}
                        </span>
                      </div>
                    </div>

                    <p className="text-muted" style={{ marginBottom: 4 }}>
                      {ehBanimento ? 'Motivo do banimento' : 'Motivo da rejeição'}: {c.motivo || 'Não especificado'}
                    </p>
                    <p style={{ marginBottom: 4 }}>{c.mensagem}</p>
                    <p className="text-muted" style={{ marginBottom: 0 }}>{formatarDataHora(c.createdAt)}</p>
                    {!c.aindaAtivo && (
                      <p className="text-muted" style={{ marginBottom: 0 }}>
                        {ehBanimento ? 'Este usuário já foi desbanido.' : 'Este estabelecimento já foi aprovado.'}
                      </p>
                    )}

                    {c.status === 'PENDENTE' && (
                      <div className="btn-row mt-16">
                        {c.aindaAtivo && ehBanimento && (
                          <button className="small" onClick={() => desbanirPelaContestacao(c)}>Desbanir usuário</button>
                        )}
                        {c.aindaAtivo && !ehBanimento && (
                          <button className="small" onClick={() => reaprovarPelaContestacao(c)}>Aprovar estabelecimento</button>
                        )}
                        <button
                          className="secondary small"
                          onClick={() => ehBanimento
                            ? marcarContestacaoAnalisada(c.id)
                            : marcarContestacaoRejeicaoEstabelecimentoAnalisada(c.id)}
                        >
                          Marcar como analisada
                        </button>
                      </div>
                    )}
                  </div>
                )
              })}

              <Paginacao
                paginaAtual={paginaContestacoes + 1}
                totalPaginas={contestacoesPage.totalPages}
                aoMudar={(p) => carregarContestacoes(p - 1)}
                ariaLabel="Paginação de contestações"
              />
            </>
          )}
        </div>
      )}

      {aba === 'auditoria' && (
        <div className="card">
          <form onSubmit={aplicarFiltroAuditoria} className="form-row" style={{ alignItems: 'flex-end' }}>
            <div className="form-group" style={{ position: 'relative' }}>
              <label>Usuário</label>
              <input
                type="text"
                placeholder="Todos (digite o nome para buscar)"
                value={buscaUsuario}
                onChange={(e) => digitarBuscaUsuario(e.target.value)}
                onFocus={() => setMostrarSugestoesUsuario(true)}
                onBlur={() => setTimeout(() => setMostrarSugestoesUsuario(false), 150)}
              />
              {mostrarSugestoesUsuario && sugestoesUsuario.length > 0 && (
                <ul
                  style={{
                    position: 'absolute', zIndex: 20, top: '100%', left: 0, right: 0,
                    margin: '4px 0 0', padding: 0, listStyle: 'none',
                    background: 'var(--bg-elevated)', border: '1px solid var(--border)',
                    borderRadius: 8, maxHeight: 220, overflowY: 'auto',
                  }}
                >
                  {sugestoesUsuario.map((u) => (
                    <li
                      key={u.id}
                      onMouseDown={() => selecionarSugestaoUsuario(u)}
                      style={{ padding: '8px 12px', cursor: 'pointer' }}
                    >
                      {u.nome} <span className="text-muted">({mascararEmail(u.email)})</span>
                    </li>
                  ))}
                </ul>
              )}
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
                  <thead><tr><th>Data</th><th>Usuário</th><th>ID do usuário</th><th>Ação</th><th>Entidade</th><th>Detalhes</th></tr></thead>
                  <tbody>
                    {auditoria.content.map((log) => (
                      <tr key={log.id}>
                        <td style={{ whiteSpace: 'nowrap' }}>{formatarDataHora(log.criadoEm)}</td>
                        <td>{log.usuarioNome || '—'}</td>
                        <td className="text-muted" style={{ fontFamily: 'monospace', fontSize: '0.85em' }}>{log.usuarioId || '—'}</td>
                        <td>{formatarAcao(log.acao)}</td>
                        <td className="text-muted">{formatarEntidade(log.entidadeTipo)}</td>
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

      {usuarioBanindo && (
        <MotivoModal
          titulo="Banir usuário"
          mensagem={`Informe o motivo do banimento de "${usuarioBanindo.nome}". Ele(a) receberá um e-mail com o motivo e um link para contestar.`}
          placeholder="Ex.: violou os termos de uso fazendo..."
          textoConfirmar="Banir"
          maxLength={500}
          confirmando={banindo}
          onConfirmar={confirmarBanimento}
          onCancelar={() => setUsuarioBanindo(null)}
        />
      )}

      {estabelecimentoRejeitando && (
        <MotivoModal
          titulo="Rejeitar estabelecimento"
          mensagem={`Informe o motivo da rejeição de "${estabelecimentoRejeitando.nome}". O dono receberá um e-mail com o motivo e um link para contestar.`}
          placeholder="Ex.: dados cadastrais incompletos..."
          textoConfirmar="Rejeitar"
          maxLength={500}
          confirmando={rejeitandoEstabelecimento}
          onConfirmar={confirmarRejeicaoEstabelecimento}
          onCancelar={() => setEstabelecimentoRejeitando(null)}
        />
      )}
    </div>
  )
}
