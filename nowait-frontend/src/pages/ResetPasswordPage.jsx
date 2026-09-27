import { useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { authService, extrairMensagemErro } from '../services/api'

export default function ResetPasswordPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') || ''

  const [novaSenha, setNovaSenha] = useState('')
  const [confirmar, setConfirmar] = useState('')
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [concluido, setConcluido] = useState(false)

  const submeter = async (e) => {
    e.preventDefault()
    setErro('')

    if (novaSenha !== confirmar) {
      setErro('As senhas não coincidem')
      return
    }

    setCarregando(true)
    try {
      await authService.resetPassword(token, novaSenha)
      setConcluido(true)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Link inválido ou expirado'))
    } finally {
      setCarregando(false)
    }
  }

  if (!token) {
    return (
      <div className="auth-wrapper">
        <div className="card text-center">
          <p className="error-box">Link inválido ou expirado.</p>
          <Link to="/esqueci-senha">
            <button style={{ width: '100%' }}>Solicitar novo link</button>
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="auth-wrapper">
      <h2>Nova senha</h2>
      <div className="card">
        {concluido ? (
          <div className="text-center">
            <p>Senha redefinida com sucesso!</p>
            <button style={{ width: '100%' }} onClick={() => navigate('/login')}>
              Fazer login
            </button>
          </div>
        ) : (
          <form onSubmit={submeter}>
            {erro && <div className="error-box">{erro}</div>}
            <div className="form-group">
              <label>Nova senha</label>
              <input
                type="password"
                value={novaSenha}
                onChange={(e) => setNovaSenha(e.target.value)}
                required
                minLength={6}
              />
              <small className="text-muted">Mínimo 6 caracteres, com maiúscula, minúscula e caractere especial.</small>
            </div>
            <div className="form-group">
              <label>Confirmar nova senha</label>
              <input
                type="password"
                value={confirmar}
                onChange={(e) => setConfirmar(e.target.value)}
                required
              />
            </div>
            <button type="submit" disabled={carregando} style={{ width: '100%' }}>
              {carregando ? 'Salvando...' : 'Redefinir senha'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
