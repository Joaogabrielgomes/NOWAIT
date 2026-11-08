import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { usuario, logout } = useAuth()
  const navigate = useNavigate()

  const sair = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="navbar">
      <Link to="/" className="brand">NOWAIT</Link>
      <nav>
        <Link to="/">Estabelecimentos</Link>

        {usuario?.role === 'CLIENTE' && (
          <Link to="/minhas-solicitacoes">Fila e agendamentos</Link>
        )}

        {usuario?.role === 'ESTABELECIMENTO' && (
          <Link to="/painel">Meu painel</Link>
        )}

        {usuario?.role === 'ADMIN' && (
          <Link to="/admin">Admin</Link>
        )}

        {usuario ? (
          <>
            <Link to="/perfil">{usuario.nome}</Link>
            <button className="secondary small" onClick={sair}>Sair</button>
          </>
        ) : (
          <>
            <Link to="/login">Entrar</Link>
            <Link to="/registro">Criar conta</Link>
          </>
        )}
      </nav>
    </header>
  )
}
