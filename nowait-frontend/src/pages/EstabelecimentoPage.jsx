import { useEffect, useState } from 'react'
import { useNavigate, useParams, Link, useLocation } from 'react-router-dom'
import { estabelecimentoService, filaService, agendamentoService, avaliacaoService, extrairMensagemErro } from '../services/api'
import { useAuth } from '../context/AuthContext'
import StarRating from '../components/StarRating'
import Paginacao from '../components/Paginacao'
import DenunciaButton from '../components/DenunciaButton'
import { corAvatar } from '../utils/avatar'
import { iconeCategoria } from '../utils/iconeCategoria'

const TAMANHO_PAGINA_AVALIACOES = 10

function formatarDataCurta(iso) {
  return new Date(iso).toLocaleDateString('pt-BR')
}

function calcularMinimoAgendamento(antecedenciaMinimaMinutos) {
  const minimo = new Date(Date.now() + antecedenciaMinimaMinutos * 60000)
  const minutosArredondados = Math.ceil(minimo.getMinutes() / 15) * 15
  minimo.setMinutes(minutosArredondados, 0, 0)
  return new Date(minimo.getTime() - minimo.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}

export default function EstabelecimentoPage() {
  const { id } = useParams()
  const { usuario } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [estabelecimento, setEstabelecimento] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  const [quantidadeFila, setQuantidadeFila] = useState(2)
  const [entrandoFila, setEntrandoFila] = useState(false)
  const [resumoFila, setResumoFila] = useState(null)

  const [quantidadeAgendamento, setQuantidadeAgendamento] = useState(2)
  const [dataHora, setDataHora] = useState('')
  const [agendando, setAgendando] = useState(false)

  const [resumoAvaliacoes, setResumoAvaliacoes] = useState(null)
  const [avaliacoesPage, setAvaliacoesPage] = useState({ content: [], totalPages: 0 })
  const [paginaAvaliacoes, setPaginaAvaliacoes] = useState(0)

  const carregarAvaliacoes = (pagina = 0) => {
    avaliacaoService.listarPorEstabelecimento(id, { pagina, tamanho: TAMANHO_PAGINA_AVALIACOES })
      .then(({ data }) => { setAvaliacoesPage(data); setPaginaAvaliacoes(pagina) })
      .catch(() => {})
  }

  useEffect(() => {
    estabelecimentoService.buscarPorId(id)
      .then(({ data }) => setEstabelecimento(data))
      .catch(() => setErro('Estabelecimento não encontrado'))
      .finally(() => setCarregando(false))

    avaliacaoService.resumoPublico(id).then(({ data }) => setResumoAvaliacoes(data)).catch(() => {})
    carregarAvaliacoes(0)
  }, [id])

  useEffect(() => {
    const pessoas = Number(quantidadeFila)
    if (!id || !pessoas || pessoas < 1) { setResumoFila(null); return }

    let ativo = true
    filaService.resumoPublico(id, pessoas)
      .then(({ data }) => { if (ativo) setResumoFila(data) })
      .catch(() => { if (ativo) setResumoFila(null) })
    return () => { ativo = false }
  }, [id, quantidadeFila])

  const entrarNaFila = async () => {
    setErro(''); setSucesso('')
    setEntrandoFila(true)
    try {
      await filaService.entrar(id, Number(quantidadeFila))
      setSucesso('Você entrou na fila! Acompanhe sua posição em "Minha fila".')
      setTimeout(() => navigate('/minhas-solicitacoes'), 1200)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível entrar na fila'))
    } finally {
      setEntrandoFila(false)
    }
  }

  const criarAgendamento = async (e) => {
    e.preventDefault()
    setErro(''); setSucesso('')
    setAgendando(true)
    try {
      await agendamentoService.criar(id, {
        quantidadePessoas: Number(quantidadeAgendamento),
        dataHora,
      })
      setSucesso('Solicitação de agendamento enviada! Aguarde a confirmação do estabelecimento.')
      setTimeout(() => navigate('/minhas-solicitacoes'), 1200)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível criar o agendamento'))
    } finally {
      setAgendando(false)
    }
  }

  if (carregando) return <div className="container"><p className="text-muted">Carregando...</p></div>
  if (!estabelecimento) return <div className="container"><div className="error-box">{erro}</div></div>

  const podeAgir = usuario?.role === 'CLIENTE'
  const IconeCategoria = iconeCategoria(estabelecimento.categoria)

  return (
    <div className="container">
      <div
        className="estabelecimento-banner"
        style={!estabelecimento.bannerUrl ? { background: corAvatar(estabelecimento.nome) } : undefined}
      >
        {estabelecimento.bannerUrl && <img src={estabelecimento.bannerUrl} alt="" />}
        <div className="estabelecimento-banner-overlay">
          {estabelecimento.logoUrl ? (
            <img src={estabelecimento.logoUrl} alt="" className="estabelecimento-banner-logo" />
          ) : (
            <div className="estabelecimento-banner-logo" style={{ background: corAvatar(estabelecimento.nome) }}>
              <IconeCategoria size={26} strokeWidth={2} />
            </div>
          )}
          <h2>{estabelecimento.nome}</h2>
        </div>
      </div>

      <p className="text-muted">
        {estabelecimento.endereco.logradouro}, {estabelecimento.endereco.numero}
        {estabelecimento.endereco.complemento ? ` — ${estabelecimento.endereco.complemento}` : ''}
        {' — '}{estabelecimento.endereco.bairro}, {estabelecimento.endereco.cidade}/{estabelecimento.endereco.estado}
        {estabelecimento.telefone ? ` — ${estabelecimento.telefone}` : ''}
      </p>
      <span className={`badge ${estabelecimento.aberto ? 'CHAMADO' : 'CANCELADO'}`}>
        {estabelecimento.aberto ? 'Aberto' : 'Fechado'}
      </span>
      <span className="text-muted" style={{ marginLeft: 12 }}>
        {estabelecimento.horarioAbertura?.slice(0, 5)} às {estabelecimento.horarioFechamento?.slice(0, 5)}
      </span>
      <span className="text-muted" style={{ marginLeft: 12 }}>
        {String(estabelecimento.mesasDisponiveis).padStart(2, '0')}/{String(estabelecimento.totalMesas).padStart(2, '0')} mesas disponíveis
      </span>

      {usuario && (
        <span style={{ marginLeft: 12 }}>
          <DenunciaButton
            tipoAlvo="ESTABELECIMENTO"
            alvoId={estabelecimento.id}
            rotulo="Denunciar estabelecimento"
            titulo="Denunciar estabelecimento"
            mensagem={`Conte para a nossa equipe por que "${estabelecimento.nome}" deve ser denunciado.`}
          />
        </span>
      )}

      {resumoAvaliacoes && resumoAvaliacoes.totalAvaliacoes > 0 && (
        <div className="btn-row mt-16" style={{ alignItems: 'center' }}>
          <StarRating valor={resumoAvaliacoes.mediaNota} somenteLeitura tamanho="1.1rem" />
          <span className="text-muted">
            {resumoAvaliacoes.mediaNota.toFixed(1)} ({resumoAvaliacoes.totalAvaliacoes} avaliação
            {resumoAvaliacoes.totalAvaliacoes > 1 ? 'ões' : ''})
          </span>
        </div>
      )}

      {erro && <div className="error-box mt-16">{erro}</div>}
      {sucesso && <div className="info-box mt-16">{sucesso}</div>}

      {!usuario && (
        <div className="mt-16">
          <p className="text-muted mb-16">Entre com sua conta de cliente para entrar na fila ou agendar um horário.</p>
          <Link to="/login" state={{ from: location.pathname }}>
            <button>Entrar</button>
          </Link>
        </div>
      )}

      {podeAgir && estabelecimento.aberto && (
        <div className="grid mt-16">
          <div className="card">
            <h3 style={{ color: 'var(--primary)' }}>Entrar na fila de espera</h3>
            <div className="form-group">
              <label>Quantidade de pessoas</label>
              <input
                type="number" min={1} max={50}
                value={quantidadeFila}
                onChange={(e) => setQuantidadeFila(e.target.value)}
                required
              />
            </div>
            {resumoFila && (
              <p className="text-muted" style={{ marginTop: -4, marginBottom: 12 }}>
                {resumoFila.quantidadeAguardando === 0
                  ? 'Ninguém na fila agora.'
                  : `${resumoFila.quantidadeAguardando} grupo(s) na fila.`}{' '}
                {resumoFila.tempoEsperaEstimadoMinutos == null
                  ? 'Mesa compatível deve estar disponível em breve.'
                  : `Espera estimada para ${quantidadeFila} pessoa(s): ~${resumoFila.tempoEsperaEstimadoMinutos} min.`}
              </p>
            )}
            <button
              onClick={entrarNaFila}
              disabled={entrandoFila || !quantidadeFila || Number(quantidadeFila) < 1 || Number(quantidadeFila) > 50}
            >
              {entrandoFila ? 'Entrando...' : 'Entrar na fila'}
            </button>
          </div>

          <div className="card">
            <h3 style={{ color: 'var(--primary)' }}>Agendar horário</h3>
            <form onSubmit={criarAgendamento}>
              <div className="form-group">
                <label>Quantidade de pessoas</label>
                <input
                  type="number" min={1} max={50}
                  value={quantidadeAgendamento}
                  onChange={(e) => setQuantidadeAgendamento(e.target.value)}
                  required
                />
              </div>
              <div className="form-group">
                <label>Data e hora</label>
                <input
                  type="datetime-local"
                  value={dataHora}
                  onChange={(e) => setDataHora(e.target.value)}
                  min={calcularMinimoAgendamento(estabelecimento.antecedenciaMinimaMinutos)}
                  step={900}
                  required
                />
                <p className="text-muted" style={{ fontSize: '0.78rem', marginTop: 4, marginBottom: 0 }}>
                  Horários disponíveis apenas em intervalos de 15 minutos (ex: 10:00, 10:15, 10:30...)
                </p>
              </div>
              <button type="submit" disabled={agendando}>
                {agendando ? 'Agendando...' : 'Agendar'}
              </button>
            </form>
          </div>
        </div>
      )}

      {avaliacoesPage.content.length > 0 && (
        <div className="mt-16">
          <h3>Avaliações</h3>
          {avaliacoesPage.content.map((av) => (
            <div className="card" key={av.id}>
              <div className="btn-row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
                <strong>{av.clienteNome}</strong>
                <StarRating valor={av.nota} somenteLeitura tamanho="1rem" />
              </div>
              {av.comentario && <p style={{ marginBottom: 0 }}>{av.comentario}</p>}
              <div className="btn-row" style={{ justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
                <p className="text-muted" style={{ fontSize: '0.78rem', marginBottom: 0 }}>
                  {formatarDataCurta(av.createdAt)}
                </p>
                {usuario && (
                  <DenunciaButton
                    tipoAlvo="AVALIACAO"
                    alvoId={av.id}
                    rotulo="Denunciar"
                    titulo="Denunciar avaliação"
                    mensagem={`Conte para a nossa equipe por que a avaliação de "${av.clienteNome}" deve ser denunciada.`}
                  />
                )}
              </div>
            </div>
          ))}

          <Paginacao
            paginaAtual={paginaAvaliacoes + 1}
            totalPaginas={avaliacoesPage.totalPages}
            aoMudar={(p) => carregarAvaliacoes(p - 1)}
            ariaLabel="Paginação de avaliações"
          />
        </div>
      )}
    </div>
  )
}
