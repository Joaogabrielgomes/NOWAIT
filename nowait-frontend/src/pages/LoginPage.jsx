import { useState } from 'react'
import { useLocation, useNavigate, Link } from 'react-router-dom'
import { authService, extrairMensagemErro } from '../services/api'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const submeter = async (e) => {
    e.preventDefault()
    setErro('')
    setCarregando(true)
    try {
      const { data } = await authService.login(email, senha)
      login(data)
      navigate(location.state?.from || '/')
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível entrar'))
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="auth-wrapper">
      <h2>Entrar</h2>
      <div className="card">
        {erro && <div className="error-box">{erro}</div>}
        <form onSubmit={submeter}>
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Senha</label>
            <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required />
          </div>
          <button type="submit" disabled={carregando} style={{ width: '100%' }}>
            {carregando ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
        <p className="text-center mt-16">
          <Link to="/esqueci-senha" className="text-muted">Esqueci minha senha</Link>
        </p>
      </div>
      <p className="text-center text-muted">
        Não tem conta? <Link to="/registro">Criar conta</Link>
      </p>
    </div>
  )
}
