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
  atualizarDadosLocais: (dadosPerfil) => {
    const atual = sessionHelper.get()
    if (!atual) return null
    const atualizado = { ...atual, nome: dadosPerfil.nome, email: dadosPerfil.email }
    localStorage.setItem('nowait_usuario', JSON.stringify(atualizado))
    return atualizado
  },
}

export const authService = {
  login:    (email, senha) => api.post('/auth/login', { email, senha }),
  registrar: (nome, email, senha, role, aceiteTermos) =>
              api.post('/auth/register', { nome, email, senha, role, aceiteTermos }),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  resetPassword:  (token, novaSenha) => api.post('/auth/reset-password', { token, novaSenha }),
}

export const usuarioService = {
  meuPerfil:        () => api.get('/usuarios/me'),
  atualizarPerfil:  (dto) => api.put('/usuarios/me', dto),
  excluirConta:     () => api.delete('/usuarios/me'),
}

export const cepService = {
  buscar: (cep) => api.get(`/cep/${cep}`),
}

export const estabelecimentoService = {
  listarTodos:  ()      => api.get('/estabelecimentos'),
  buscarPorId:  (id)    => api.get(`/estabelecimentos/${id}`),
  meuEstabelecimento: () => api.get('/estabelecimentos/me'),
  criar:        (dto)   => api.post('/estabelecimentos', dto),
  atualizar:    (id, dto) => api.put(`/estabelecimentos/${id}`, dto),
  buscarPorNome: (nome) => api.get('/estabelecimentos/search', { params: { nome } }),
}

export const mesaService = {
  listar:          (estabelecimentoId) => api.get(`/estabelecimentos/${estabelecimentoId}/mesas`),
  criar:           (estabelecimentoId, dto) => api.post(`/estabelecimentos/${estabelecimentoId}/mesas`, dto),
  atualizarStatus: (mesaId, status) => api.put(`/mesas/${mesaId}`, { status }),
  liberar:         (mesaId) => api.patch(`/mesas/${mesaId}/liberar`),
  remover:         (mesaId) => api.delete(`/mesas/${mesaId}`),
}

export const filaService = {
  entrar:           (estabelecimentoId, quantidadePessoas) =>
                       api.post(`/estabelecimentos/${estabelecimentoId}/fila`, { quantidadePessoas }),
  listar:           (estabelecimentoId) => api.get(`/estabelecimentos/${estabelecimentoId}/fila`),
  minhaEntrada:     () => api.get('/fila/me/ativa'),
  chamar:           (filaId) => api.patch(`/fila/${filaId}/chamar`),
  atender:          (filaId) => api.patch(`/fila/${filaId}/atender`),
  cancelar:         (filaId) => api.patch(`/fila/${filaId}/cancelar`),
  resumoPublico:    (estabelecimentoId) => api.get(`/estabelecimentos/${estabelecimentoId}/fila/resumo`),
  meuHistorico:     () => api.get('/fila/me/historico'),
}

export const favoritoService = {
  listarMeus: ()     => api.get('/favoritos'),
  listarIds:  ()     => api.get('/favoritos/ids'),
  adicionar:  (estabelecimentoId) => api.post(`/estabelecimentos/${estabelecimentoId}/favorito`),
  remover:    (estabelecimentoId) => api.delete(`/estabelecimentos/${estabelecimentoId}/favorito`),
}

export const agendamentoService = {
  criar:              (estabelecimentoId, dto) => api.post(`/estabelecimentos/${estabelecimentoId}/agendamentos`, dto),
  listarPorEstabelecimento: (estabelecimentoId) => api.get(`/estabelecimentos/${estabelecimentoId}/agendamentos`),
  listarMeus:         () => api.get('/agendamentos/me'),
  aceitar:            (id) => api.patch(`/agendamentos/${id}/aceitar`),
  recusar:            (id, motivo) => api.patch(`/agendamentos/${id}/recusar`, { motivo }),
  cancelar:           (id, motivo) => api.patch(`/agendamentos/${id}/cancelar`, motivo ? { motivo } : undefined),
  confirmarChegada:   (id) => api.patch(`/agendamentos/${id}/confirmar-chegada`),
}

export const avaliacaoService = {
  avaliar:            (dto) => api.post('/avaliacoes', dto),
  listarPendentes:    () => api.get('/avaliacoes/pendentes'),
  listarPorEstabelecimento: (estabelecimentoId) => api.get(`/estabelecimentos/${estabelecimentoId}/avaliacoes`),
  resumoPublico:      (estabelecimentoId) => api.get(`/estabelecimentos/${estabelecimentoId}/avaliacoes/resumo`),
}

export const adminService = {
  listarUsuarios:        () => api.get('/admin/usuarios'),
  removerUsuario:        (id) => api.delete(`/admin/usuarios/${id}`),
  listarEstabelecimentos: () => api.get('/admin/estabelecimentos'),
  dashboard:             () => api.get('/admin/dashboard'),
  auditoria:             (params) => api.get('/admin/auditoria', { params }),
  exportarAuditoria:     (params) => api.get('/admin/auditoria/export', { params, responseType: 'blob' }),
}

export default api
