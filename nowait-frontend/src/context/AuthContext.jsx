import { createContext, useContext, useState } from 'react'
import { sessionHelper } from '../services/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(sessionHelper.get())

  const login = (loginResponseData) => {
    sessionHelper.save(loginResponseData)
    setUsuario(sessionHelper.get())
  }

  const logout = () => {
    sessionHelper.clear()
    setUsuario(null)
  }

  const atualizarUsuario = (dadosPerfil) => {
    const atualizado = sessionHelper.atualizarDadosLocais(dadosPerfil)
    setUsuario(atualizado)
  }

  return (
    <AuthContext.Provider value={{ usuario, login, logout, atualizarUsuario }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
