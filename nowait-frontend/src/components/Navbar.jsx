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
          <>
            <Link to="/minha-fila">Minha fila</Link>
            <Link to="/meus-agendamentos">Meus agendamentos</Link>
          </>
        )}

        {usuario?.role === 'ESTABELECIMENTO' && (
          <Link to="/painel">Meu painel</Link>
        )}

        {usuario?.role === 'ADMIN' && (
          <Link to="/admin">Admin</Link>
        )}

        {usuario ? (
          <>
            <span className="user-tag">{usuario.nome}</span>
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
