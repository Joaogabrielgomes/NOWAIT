import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import ErrorBoundary from './components/ErrorBoundary'
import Navbar from './components/Navbar'

import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import ResetPasswordPage from './pages/ResetPasswordPage'
import HomePage from './pages/HomePage'
import EstabelecimentoPage from './pages/EstabelecimentoPage'
import MinhaFilaPage from './pages/MinhaFilaPage'
import MeusAgendamentosPage from './pages/MeusAgendamentosPage'
import CadastrarEstabelecimentoPage from './pages/CadastrarEstabelecimentoPage'
import PainelEstabelecimentoPage from './pages/PainelEstabelecimentoPage'
import AdminPage from './pages/AdminPage'

function RotaPrivada({ children, rolesPermitidos }) {
  const { usuario } = useAuth()
  const location = useLocation()

  if (!usuario) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }
  if (rolesPermitidos && !rolesPermitidos.includes(usuario.role)) {
    return <Navigate to="/" replace />
  }
  return children
}

function AppRoutes() {
  return (
    <BrowserRouter>
      <Navbar />
      <ErrorBoundary>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/registro" element={<RegisterPage />} />
          <Route path="/esqueci-senha" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/estabelecimentos/:id" element={<EstabelecimentoPage />} />

          <Route path="/minha-fila" element={
            <RotaPrivada rolesPermitidos={['CLIENTE']}><MinhaFilaPage /></RotaPrivada>
          } />
          <Route path="/meus-agendamentos" element={
            <RotaPrivada rolesPermitidos={['CLIENTE']}><MeusAgendamentosPage /></RotaPrivada>
          } />

          <Route path="/painel/cadastro" element={
            <RotaPrivada rolesPermitidos={['ESTABELECIMENTO']}><CadastrarEstabelecimentoPage /></RotaPrivada>
          } />
          <Route path="/painel" element={
            <RotaPrivada rolesPermitidos={['ESTABELECIMENTO']}><PainelEstabelecimentoPage /></RotaPrivada>
          } />

          <Route path="/admin" element={
            <RotaPrivada rolesPermitidos={['ADMIN']}><AdminPage /></RotaPrivada>
          } />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </ErrorBoundary>
    </BrowserRouter>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}
