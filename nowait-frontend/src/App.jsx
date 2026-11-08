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
import MinhasSolicitacoesPage from './pages/MinhasSolicitacoesPage'
import CadastrarEstabelecimentoPage from './pages/CadastrarEstabelecimentoPage'
import PainelEstabelecimentoPage from './pages/PainelEstabelecimentoPage'
import AdminPage from './pages/AdminPage'
import PerfilPage from './pages/PerfilPage'

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

          <Route path="/perfil" element={
            <RotaPrivada><PerfilPage /></RotaPrivada>
          } />

          <Route path="/minhas-solicitacoes" element={
            <RotaPrivada rolesPermitidos={['CLIENTE']}><MinhasSolicitacoesPage /></RotaPrivada>
          } />
          <Route path="/minha-fila" element={<Navigate to="/minhas-solicitacoes" replace />} />
          <Route path="/meus-agendamentos" element={<Navigate to="/minhas-solicitacoes" replace />} />

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
