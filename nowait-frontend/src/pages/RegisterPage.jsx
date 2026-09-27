import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authService, extrairMensagemErro } from '../services/api'
import { useAuth } from '../context/AuthContext'
import TermosModal from '../components/TermosModal'

export default function RegisterPage() {
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [role, setRole] = useState('CLIENTE')
  const [aceiteTermos, setAceiteTermos] = useState(false)
  const [mostrarTermos, setMostrarTermos] = useState(false)
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()

  const submeter = async (e) => {
    e.preventDefault()
    setErro('')
    setCarregando(true)
    try {
      const { data } = await authService.registrar(nome, email, senha, role, aceiteTermos)
      login(data)
      navigate(role === 'ESTABELECIMENTO' ? '/painel/cadastro' : '/')
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível criar a conta'))
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="auth-wrapper">
      <h2>Criar conta</h2>
      <div className="card">
        {erro && <div className="error-box">{erro}</div>}
        <form onSubmit={submeter}>
          <div className="form-group">
            <label>Nome</label>
            <input value={nome} onChange={(e) => setNome(e.target.value)} required maxLength={100} />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Senha</label>
            <input
              type="password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              required
              minLength={6}
            />
            <small className="text-muted">Mínimo 6 caracteres, com maiúscula, minúscula e caractere especial.</small>
          </div>
          <div className="form-group">
            <label>Eu sou</label>
            <select value={role} onChange={(e) => setRole(e.target.value)}>
              <option value="CLIENTE">Cliente — quero entrar em filas e agendar</option>
              <option value="ESTABELECIMENTO">Estabelecimento — quero gerenciar meu restaurante</option>
            </select>
          </div>

          <div className="form-group">
            <label style={{ display: 'flex', alignItems: 'flex-start', gap: 8, cursor: 'pointer' }}>
              <input
                type="checkbox"
                style={{ width: 'auto', marginTop: 3 }}
                checked={aceiteTermos}
                onChange={(e) => setAceiteTermos(e.target.checked)}
                required
              />
              <span>
                Li e aceito os{' '}
                <button type="button" className="link-button" onClick={() => setMostrarTermos(true)}>
                  Termos de Uso e a Política de Privacidade
                </button>
              </span>
            </label>
          </div>

          <button type="submit" disabled={carregando} style={{ width: '100%' }}>
            {carregando ? 'Criando conta...' : 'Criar conta'}
          </button>
        </form>
      </div>
      <p className="text-center text-muted">
        Já tem conta? <Link to="/login">Entrar</Link>
      </p>

      {mostrarTermos && <TermosModal onClose={() => setMostrarTermos(false)} />}
    </div>
  )
}
