import { useEffect, useState } from 'react'
import { adminService, extrairMensagemErro } from '../services/api'
import StatusBadge from '../components/StatusBadge'

export default function AdminPage() {
  const [usuarios, setUsuarios] = useState([])
  const [estabelecimentos, setEstabelecimentos] = useState([])
  const [aba, setAba] = useState('usuarios')
  const [erro, setErro] = useState('')

  const carregar = () => {
    adminService.listarUsuarios().then(({ data }) => setUsuarios(data))
    adminService.listarEstabelecimentos().then(({ data }) => setEstabelecimentos(data))
  }

  useEffect(() => { carregar() }, [])

  const removerUsuario = async (id) => {
    setErro('')
    try {
      await adminService.removerUsuario(id)
      carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível remover o usuário'))
    }
  }

  return (
    <div className="container">
      <h2>Administração</h2>
      {erro && <div className="error-box">{erro}</div>}

      <div className="btn-row mb-16">
        <button className={aba === 'usuarios' ? '' : 'secondary'} onClick={() => setAba('usuarios')}>
          Usuários ({usuarios.length})
        </button>
        <button className={aba === 'estabelecimentos' ? '' : 'secondary'} onClick={() => setAba('estabelecimentos')}>
          Estabelecimentos ({estabelecimentos.length})
        </button>
      </div>

      {aba === 'usuarios' && (
        <table>
          <thead><tr><th>Nome</th><th>Email</th><th>Perfil</th><th></th></tr></thead>
          <tbody>
            {usuarios.map((u) => (
              <tr key={u.id}>
                <td>{u.nome}</td>
                <td>{u.email}</td>
                <td><StatusBadge status={u.role} /></td>
                <td>
                  {u.role !== 'ADMIN' && (
                    <button className="danger small" onClick={() => removerUsuario(u.id)}>Remover</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {aba === 'estabelecimentos' && (
        <table>
          <thead><tr><th>Nome</th><th>Cidade</th><th>Status</th></tr></thead>
          <tbody>
            {estabelecimentos.map((e) => (
              <tr key={e.id}>
                <td>{e.nome}</td>
                <td>{e.endereco.cidade}/{e.endereco.estado}</td>
                <td><span className={`badge ${e.aberto ? 'CHAMADO' : 'CANCELADO'}`}>{e.aberto ? 'Aberto' : 'Fechado'}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
