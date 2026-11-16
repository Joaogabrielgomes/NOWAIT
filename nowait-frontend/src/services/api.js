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
  atualizarPerfil:  (dto) => api.put('/usuarios/me', dto),
  excluirConta:     () => api.delete('/usuarios/me'),
}

export const cepService = {
  buscar: (cep) => api.get(`/cep/${cep}`),
}

function enviarImagem(caminho, arquivo) {
  const formData = new FormData()
  formData.append('arquivo', arquivo)
  return api.post(caminho, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export const estabelecimentoService = {
  listar:       (params) => api.get('/estabelecimentos', { params }),
  buscarPorId:  (id)    => api.get(`/estabelecimentos/${id}`),
  meuEstabelecimento: () => api.get('/estabelecimentos/me'),
  criar:        (dto)   => api.post('/estabelecimentos', dto),
  atualizar:    (id, dto) => api.put(`/estabelecimentos/${id}`, dto),
  alternarAberto: (id) => api.patch(`/estabelecimentos/${id}/aberto`),
  enviarLogo:   (id, arquivo) => enviarImagem(`/estabelecimentos/${id}/logo`, arquivo),
  enviarBanner: (id, arquivo) => enviarImagem(`/estabelecimentos/${id}/banner`, arquivo),
  removerLogo:  (id) => api.delete(`/estabelecimentos/${id}/logo`),
  removerBanner: (id) => api.delete(`/estabelecimentos/${id}/banner`),
}

export const mesaService = {
  listar:          (estabelecimentoId) => api.get(`/estabelecimentos/${estabelecimentoId}/mesas`),
  criar:           (estabelecimentoId, dto) => api.post(`/estabelecimentos/${estabelecimentoId}/mesas`, dto),
  atualizarStatus: (mesaId, status) => api.put(`/mesas/${mesaId}`, { status }),
  atualizarCapacidade: (mesaId, capacidade) => api.patch(`/mesas/${mesaId}/capacidade`, { capacidade }),
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
  resumoPublico:    (estabelecimentoId, pessoas) => api.get(`/estabelecimentos/${estabelecimentoId}/fila/resumo`, { params: pessoas ? { pessoas } : {} }),
  resumoPublicoLote: (ids) => api.get('/fila/resumo-lote', { params: { ids: ids.join(',') } }),
  meuHistorico:     (params) => api.get('/fila/me/historico', { params }),
}

export const favoritoService = {
  listarMeus: ()     => api.get('/favoritos'),
  listarIds:  ()     => api.get('/favoritos/ids'),
  adicionar:  (estabelecimentoId) => api.post(`/estabelecimentos/${estabelecimentoId}/favorito`),
  remover:    (estabelecimentoId) => api.delete(`/estabelecimentos/${estabelecimentoId}/favorito`),
}

export const agendamentoService = {
  criar:              (estabelecimentoId, dto) => api.post(`/estabelecimentos/${estabelecimentoId}/agendamentos`, dto),
  listarPorEstabelecimento: (estabelecimentoId, params) => api.get(`/estabelecimentos/${estabelecimentoId}/agendamentos`, { params }),
  listarMeus:         (params) => api.get('/agendamentos/me', { params }),
  aceitar:            (id) => api.patch(`/agendamentos/${id}/aceitar`),
  recusar:            (id, motivo) => api.patch(`/agendamentos/${id}/recusar`, { motivo }),
  cancelar:           (id, motivo) => api.patch(`/agendamentos/${id}/cancelar`, motivo ? { motivo } : undefined),
  confirmarChegada:   (id) => api.patch(`/agendamentos/${id}/confirmar-chegada`),
}

export const avaliacaoService = {
  avaliar:            (dto) => api.post('/avaliacoes', dto),
  editar:             (id, dto) => api.put(`/avaliacoes/${id}`, dto),
  listarPendentes:    () => api.get('/avaliacoes/pendentes'),
  listarMinhas:       (params) => api.get('/avaliacoes/minhas', { params }),
  listarPorEstabelecimento: (estabelecimentoId, params) => api.get(`/estabelecimentos/${estabelecimentoId}/avaliacoes`, { params }),
  resumoPublico:      (estabelecimentoId) => api.get(`/estabelecimentos/${estabelecimentoId}/avaliacoes/resumo`),
  resumoPublicoLote:  (ids) => api.get('/avaliacoes/resumo-lote', { params: { ids: ids.join(',') } }),
}

export const adminService = {
  listarUsuarios:        (params) => api.get('/admin/usuarios', { params }),
  removerUsuario:        (id) => api.delete(`/admin/usuarios/${id}`),
  banirUsuario:          (id, motivo) => api.patch(`/admin/usuarios/${id}/banir`, { motivo }),
  desbanirUsuario:       (id) => api.patch(`/admin/usuarios/${id}/desbanir`),
  listarEstabelecimentos: (params) => api.get('/admin/estabelecimentos', { params }),
  aprovarEstabelecimento: (id) => api.patch(`/admin/estabelecimentos/${id}/aprovar`),
  rejeitarEstabelecimento: (id, motivo) => api.patch(`/admin/estabelecimentos/${id}/rejeitar`, { motivo }),
  dashboard:             () => api.get('/admin/dashboard'),
  auditoria:             (params) => api.get('/admin/auditoria', { params }),
  exportarAuditoria:     (params) => api.get('/admin/auditoria/export', { params, responseType: 'blob' }),
  listarDenuncias:       (params) => api.get('/admin/denuncias', { params }),
  resolverDenuncia:      (id, dto) => api.patch(`/admin/denuncias/${id}/resolver`, dto),
  listarContestacoes:    (params) => api.get('/admin/contestacoes', { params }),
  marcarContestacaoAnalisada:  (id) => api.patch(`/admin/contestacoes-banimento/${id}/analisar`),
  marcarContestacaoRejeicaoEstabelecimentoAnalisada: (id) => api.patch(`/admin/contestacoes-rejeicao-estabelecimento/${id}/analisar`),
}

export const denunciaService = {
  criar: (dto) => api.post('/denuncias', dto),
}

export const banimentoService = {
  buscarContestacaoPorToken: (token) => api.get(`/contestacoes-banimento/${token}`),
  enviarContestacao: (token, mensagem) => api.post('/contestacoes-banimento', { token, mensagem }),
}

export const rejeicaoEstabelecimentoService = {
  buscarContestacaoPorToken: (token) => api.get(`/contestacoes-rejeicao-estabelecimento/${token}`),
  enviarContestacao: (token, mensagem) => api.post('/contestacoes-rejeicao-estabelecimento', { token, mensagem }),
}

export default api
