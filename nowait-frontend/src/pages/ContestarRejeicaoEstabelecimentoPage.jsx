import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { rejeicaoEstabelecimentoService, extrairMensagemErro } from '../services/api'

export default function ContestarRejeicaoEstabelecimentoPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') || ''

  const [info, setInfo] = useState(null)
  const [carregandoInfo, setCarregandoInfo] = useState(true)
  const [erroInfo, setErroInfo] = useState('')

  const [mensagem, setMensagem] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState('')
  const [enviado, setEnviado] = useState(false)

  useEffect(() => {
    if (!token) { setCarregandoInfo(false); return }
    rejeicaoEstabelecimentoService.buscarContestacaoPorToken(token)
      .then(({ data }) => setInfo(data))
      .catch((e) => setErroInfo(extrairMensagemErro(e, 'Link inválido ou expirado')))
      .finally(() => setCarregandoInfo(false))
  }, [token])

  const enviar = async (e) => {
    e.preventDefault()
    setErro('')
    if (!mensagem.trim()) { setErro('Escreva sua contestação.'); return }
    setEnviando(true)
    try {
      await rejeicaoEstabelecimentoService.enviarContestacao(token, mensagem.trim())
      setEnviado(true)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível enviar sua contestação'))
    } finally {
      setEnviando(false)
    }
  }

  if (!token || (!carregandoInfo && (erroInfo || !info))) {
    return (
      <div className="auth-wrapper">
        <div className="card text-center">
          <p className="error-box">{erroInfo || 'Link inválido ou expirado.'}</p>
          <Link to="/login">
            <button style={{ width: '100%' }}>Voltar para o login</button>
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="auth-wrapper">
      <h2>Contestar rejeição</h2>
      <div className="card">
        {carregandoInfo && <p className="text-muted">Carregando...</p>}

        {!carregandoInfo && enviado && (
          <div className="text-center">
            <p>Sua contestação foi enviada. Nossa equipe vai analisar e, se for o caso, aprovar seu estabelecimento.</p>
            <Link to="/login">
              <button style={{ width: '100%' }}>Voltar para o login</button>
            </Link>
          </div>
        )}

        {!carregandoInfo && !enviado && info && (
          <>
            <p className="text-muted" style={{ marginBottom: 4 }}>
              Estabelecimento: <strong>{info.estabelecimentoNome}</strong>
            </p>
            <p className="text-muted" style={{ marginBottom: 16 }}>
              Motivo informado pelo administrador: {info.motivoRejeicao || 'Não especificado'}
            </p>

            <form onSubmit={enviar}>
              {erro && <div className="error-box">{erro}</div>}
              <div className="form-group">
                <label>Sua contestação</label>
                <textarea
                  value={mensagem}
                  onChange={(e) => setMensagem(e.target.value)}
                  placeholder="Explique por que você acredita que a rejeição foi um engano..."
                  rows={5}
                  maxLength={1000}
                  required
                />
              </div>
              <button type="submit" disabled={enviando} style={{ width: '100%' }}>
                {enviando ? 'Enviando...' : 'Enviar contestação'}
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  )
}
