import { useState } from 'react'
import { Link } from 'react-router-dom'
import { authService, extrairMensagemErro } from '../services/api'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [enviado, setEnviado] = useState(false)

  const submeter = async (e) => {
    e.preventDefault()
    setErro('')
    setCarregando(true)
    try {
      await authService.forgotPassword(email.trim())
      setEnviado(true)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível enviar o e-mail'))
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="auth-wrapper">
      <h2>Esqueceu sua senha?</h2>
      <div className="card">
        {enviado ? (
          <div className="text-center">
            <p>
              Se o e-mail <strong>{email}</strong> estiver cadastrado, você vai receber um link
              de redefinição em instantes.
            </p>
            <p className="text-muted">Verifique também a caixa de spam.</p>
            <Link to="/login">
              <button className="mt-16" style={{ width: '100%' }}>Voltar para o login</button>
            </Link>
          </div>
        ) : (
          <form onSubmit={submeter}>
            <p className="text-muted mb-16">
              Informe seu e-mail e enviaremos um link para redefinir sua senha.
            </p>
            {erro && <div className="error-box">{erro}</div>}
            <div className="form-group">
              <label>Email</label>
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </div>
            <button type="submit" disabled={carregando} style={{ width: '100%' }}>
              {carregando ? 'Enviando...' : 'Enviar link de redefinição'}
            </button>
          </form>
        )}
      </div>
      <p className="text-center text-muted">
        <Link to="/login">Voltar para o login</Link>
      </p>
    </div>
  )
}
