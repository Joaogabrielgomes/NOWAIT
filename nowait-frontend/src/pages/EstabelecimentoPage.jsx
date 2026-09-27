import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { estabelecimentoService, filaService, agendamentoService, extrairMensagemErro } from '../services/api'
import { useAuth } from '../context/AuthContext'

export default function EstabelecimentoPage() {
  const { id } = useParams()
  const { usuario } = useAuth()
  const navigate = useNavigate()

  const [estabelecimento, setEstabelecimento] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  const [quantidadeFila, setQuantidadeFila] = useState(2)
  const [entrandoFila, setEntrandoFila] = useState(false)

  const [quantidadeAgendamento, setQuantidadeAgendamento] = useState(2)
  const [dataHora, setDataHora] = useState('')
  const [agendando, setAgendando] = useState(false)

  useEffect(() => {
    estabelecimentoService.buscarPorId(id)
      .then(({ data }) => setEstabelecimento(data))
      .catch(() => setErro('Estabelecimento não encontrado'))
      .finally(() => setCarregando(false))
  }, [id])

  const entrarNaFila = async () => {
    setErro(''); setSucesso('')
    setEntrandoFila(true)
    try {
      await filaService.entrar(id, Number(quantidadeFila))
      setSucesso('Você entrou na fila! Acompanhe sua posição em "Minha fila".')
      setTimeout(() => navigate('/minha-fila'), 1200)
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
      setSucesso('Agendamento confirmado!')
      setTimeout(() => navigate('/meus-agendamentos'), 1200)
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
      </p>
      <span className={`badge ${estabelecimento.aberto ? 'CHAMADO' : 'CANCELADO'}`}>
        {estabelecimento.aberto ? 'Aberto' : 'Fechado'}
      </span>

      {erro && <div className="error-box mt-16">{erro}</div>}
      {sucesso && <div className="info-box mt-16">{sucesso}</div>}

      {!usuario && (
        <p className="mt-16 text-muted">Entre com sua conta de cliente para entrar na fila ou agendar um horário.</p>
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
              />
            </div>
            <button onClick={entrarNaFila} disabled={entrandoFila}>
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
                />
              </div>
              <div className="form-group">
                <label>Data e hora</label>
                <input
                  type="datetime-local"
                  value={dataHora}
                  onChange={(e) => setDataHora(e.target.value)}
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
    </div>
  )
}
