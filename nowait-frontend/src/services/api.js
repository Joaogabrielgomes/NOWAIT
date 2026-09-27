import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('nowait_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('nowait_token')
      localStorage.removeItem('nowait_usuario')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

// ── Sessão ────────────────────────────────────────────────────────────────────
export function extrairMensagemErro(erro, mensagemPadrao = 'Não foi possível completar a operação') {
  const dados = erro.response?.data
  if (!dados) return mensagemPadrao
  if (dados.camposComErro) return Object.values(dados.camposComErro).join(', ')
  return dados.message || mensagemPadrao
}

export const sessionHelper = {
  save: (data) => {
    localStorage.setItem('nowait_token', data.token)
    localStorage.setItem('nowait_usuario', JSON.stringify({
      id: data.usuarioId, nome: data.nome, email: data.email, role: data.role,
    }))
  },
  get:      () => { const r = localStorage.getItem('nowait_usuario'); return r ? JSON.parse(r) : null },
  isLogged: () => !!localStorage.getItem('nowait_token'),
  clear:    () => {
    localStorage.removeItem('nowait_token')
    localStorage.removeItem('nowait_usuario')
  },
}

// ── Auth ──────────────────────────────────────────────────────────────────────
export const authService = {
  login:    (email, senha) => api.post('/auth/login', { email, senha }),
  registrar: (nome, email, senha, role, aceiteTermos) =>
              api.post('/auth/register', { nome, email, senha, role, aceiteTermos }),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  resetPassword:  (token, novaSenha) => api.post('/auth/reset-password', { token, novaSenha }),
}

export const usuarioService = {
  meuPerfil: () => api.get('/usuarios/me'),
}

// ── CEP ───────────────────────────────────────────────────────────────────────
export const cepService = {
  buscar: (cep) => api.get(`/cep/${cep}`),
}

// ── Estabelecimentos ──────────────────────────────────────────────────────────
export const estabelecimentoService = {
  listarTodos:  ()      => api.get('/estabelecimentos'),
  buscarPorId:  (id)    => api.get(`/estabelecimentos/${id}`),
  meuEstabelecimento: () => api.get('/estabelecimentos/me'),
  criar:        (dto)   => api.post('/estabelecimentos', dto),
  atualizar:    (id, dto) => api.put(`/estabelecimentos/${id}`, dto),
}

// ── Mesas ─────────────────────────────────────────────────────────────────────
export const mesaService = {
  listar:          (estabelecimentoId) => api.get(`/estabelecimentos/${estabelecimentoId}/mesas`),
  criar:           (estabelecimentoId, dto) => api.post(`/estabelecimentos/${estabelecimentoId}/mesas`, dto),
  atualizarStatus: (mesaId, status) => api.put(`/mesas/${mesaId}`, { status }),
  liberar:         (mesaId) => api.patch(`/mesas/${mesaId}/liberar`),
  remover:         (mesaId) => api.delete(`/mesas/${mesaId}`),
}

// ── Fila de espera ────────────────────────────────────────────────────────────
export const filaService = {
  entrar:           (estabelecimentoId, quantidadePessoas) =>
                       api.post(`/estabelecimentos/${estabelecimentoId}/fila`, { quantidadePessoas }),
  listar:           (estabelecimentoId) => api.get(`/estabelecimentos/${estabelecimentoId}/fila`),
  minhaEntrada:     () => api.get('/fila/me/ativa'),
  chamar:           (filaId) => api.patch(`/fila/${filaId}/chamar`),
  atender:          (filaId) => api.patch(`/fila/${filaId}/atender`),
  cancelar:         (filaId) => api.patch(`/fila/${filaId}/cancelar`),
}

// ── Agendamentos ──────────────────────────────────────────────────────────────
export const agendamentoService = {
  criar:              (estabelecimentoId, dto) => api.post(`/estabelecimentos/${estabelecimentoId}/agendamentos`, dto),
  listarPorEstabelecimento: (estabelecimentoId) => api.get(`/estabelecimentos/${estabelecimentoId}/agendamentos`),
  listarMeus:         () => api.get('/agendamentos/me'),
  cancelar:           (id) => api.patch(`/agendamentos/${id}/cancelar`),
  confirmarChegada:   (id) => api.patch(`/agendamentos/${id}/confirmar-chegada`),
}

// ── Admin ─────────────────────────────────────────────────────────────────────
export const adminService = {
  listarUsuarios:        () => api.get('/admin/usuarios'),
  removerUsuario:        (id) => api.delete(`/admin/usuarios/${id}`),
  listarEstabelecimentos: () => api.get('/admin/estabelecimentos'),
}

export default api
