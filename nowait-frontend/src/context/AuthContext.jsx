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

  return (
    <AuthContext.Provider value={{ usuario, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
