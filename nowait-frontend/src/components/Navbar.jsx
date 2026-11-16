import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'

export default function Navbar() {
  const { usuario, logout } = useAuth()
  const { tema, alternarTema } = useTheme()
  const navigate = useNavigate()
  const [menuAberto, setMenuAberto] = useState(false)
  const menuRef = useRef(null)

  useEffect(() => {
    if (!menuAberto) return

    const aoClicarFora = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setMenuAberto(false)
      }
    }
    document.addEventListener('mousedown', aoClicarFora)
    return () => document.removeEventListener('mousedown', aoClicarFora)
  }, [menuAberto])

  const deslogar = () => {
    setMenuAberto(false)
    logout()
    navigate('/login')
  }

  const irParaPerfil = () => {
    setMenuAberto(false)
    navigate('/perfil')
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

        {!usuario && (
          <>
            <Link to="/login">Entrar</Link>
            <Link to="/registro">Criar conta</Link>
          </>
        )}

        <div className="navbar-menu" ref={menuRef}>
          <button
            className="navbar-menu-trigger"
            onClick={() => setMenuAberto((v) => !v)}
            aria-label="Abrir menu"
            aria-expanded={menuAberto}
          >
            •••
          </button>

          {menuAberto && (
            <div className="navbar-menu-dropdown">
              {usuario && (
                <>
                  <button className="navbar-menu-item" onClick={irParaPerfil}>
                    {usuario.nome}
                  </button>
                  <div className="navbar-menu-divider" />
                </>
              )}

              <div className="navbar-menu-item" style={{ cursor: 'default' }}>
                <span>Modo claro</span>
                <button
                  className={`theme-switch ${tema === 'light' ? 'ativo' : ''}`}
                  onClick={alternarTema}
                  aria-label="Alternar modo claro/escuro"
                  aria-pressed={tema === 'light'}
                >
                  <span className="knob" />
                </button>
              </div>

              {usuario && (
                <>
                  <div className="navbar-menu-divider" />
                  <button className="navbar-menu-item sair" onClick={deslogar}>
                    Deslogar
                  </button>
                </>
              )}
            </div>
          )}
        </div>
      </nav>
    </header>
  )
}
