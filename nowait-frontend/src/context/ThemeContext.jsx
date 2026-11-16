import { createContext, useContext, useEffect, useState } from 'react'

const ThemeContext = createContext(null)

function temaSalvo() {
  try {
    return localStorage.getItem('nowait-theme') || 'dark'
  } catch {
    return 'dark'
  }
}

export function ThemeProvider({ children }) {
  const [tema, setTema] = useState(temaSalvo)

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', tema)
    try {
      localStorage.setItem('nowait-theme', tema)
    } catch {

    }
  }, [tema])

  const alternarTema = () => setTema((atual) => (atual === 'dark' ? 'light' : 'dark'))

  return (
    <ThemeContext.Provider value={{ tema, alternarTema }}>
      {children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  return useContext(ThemeContext)
}
