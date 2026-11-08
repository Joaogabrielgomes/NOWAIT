import { useEffect, useState } from 'react'
import { useNavigate, useParams, Link, useLocation } from 'react-router-dom'
import { estabelecimentoService, filaService, agendamentoService, avaliacaoService, extrairMensagemErro } from '../services/api'
import { useAuth } from '../context/AuthContext'
import StarRating from '../components/StarRating'

function formatarDataCurta(iso) {
  return new Date(iso).toLocaleDateString('pt-BR')
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

  const [quantidadeAgendamento, setQuantidadeAgendamento] = useState(2)
  const [dataHora, setDataHora] = useState('')
  const [agendando, setAgendando] = useState(false)

  const [resumoAvaliacoes, setResumoAvaliacoes] = useState(null)
  const [avaliacoes, setAvaliacoes] = useState([])

  useEffect(() => {
    estabelecimentoService.buscarPorId(id)
      .then(({ data }) => setEstabelecimento(data))
      .catch(() => setErro('Estabelecimento não encontrado'))
      .finally(() => setCarregando(false))

    avaliacaoService.resumoPublico(id).then(({ data }) => setResumoAvaliacoes(data)).catch(() => {})
    avaliacaoService.listarPorEstabelecimento(id).then(({ data }) => setAvaliacoes(data)).catch(() => {})
  }, [id])

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

  return (
    <div className="container">
      <h2>{estabelecimento.nome}</h2>
      <p className="text-muted">
        {estabelecimento.endereco.logradouro}, {estabelecimento.endereco.numero}
        {estabelecimento.endereco.complemento ? ` — ${estabelecimento.endereco.complemento}` : ''}
        {' — '}{estabelecimento.endereco.bairro}, {estabelecimento.endereco.cidade}/{estabelecimento.endereco.estado}
        {estabelecimento.telefone ? ` — ${estabelecimento.telefone}` : ''}
      </p>
      <span className={`badge ${estabelecimento.aberto ? 'CHAMADO' : 'CANCELADO'}`}>
        {estabelecimento.aberto ? 'Aberto' : 'Fechado'}
      </span>

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
            <h3>Entrar na fila de espera</h3>
            <div className="form-group">
              <label>Quantidade de pessoas</label>
              <input
                type="number" min={1} max={50}
                value={quantidadeFila}
                onChange={(e) => setQuantidadeFila(e.target.value)}
                required
              />
            </div>
            <button
              onClick={entrarNaFila}
              disabled={entrandoFila || !quantidadeFila || Number(quantidadeFila) < 1 || Number(quantidadeFila) > 50}
            >
              {entrandoFila ? 'Entrando...' : 'Entrar na fila'}
            </button>
          </div>

          <div className="card">
            <h3>Agendar horário</h3>
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
                  min={new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16)}
                  required
                />
              </div>
              <button type="submit" disabled={agendando}>
                {agendando ? 'Agendando...' : 'Agendar'}
              </button>
            </form>
          </div>
        </div>
      )}

      {avaliacoes.length > 0 && (
        <div className="mt-16">
          <h3>Avaliações</h3>
          {avaliacoes.map((av) => (
            <div className="card" key={av.id}>
              <div className="btn-row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
                <strong>{av.clienteNome}</strong>
                <StarRating valor={av.nota} somenteLeitura tamanho="1rem" />
              </div>
              {av.comentario && <p style={{ marginBottom: 0 }}>{av.comentario}</p>}
              <p className="text-muted" style={{ fontSize: '0.78rem', marginBottom: 0 }}>
                {formatarDataCurta(av.createdAt)}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
