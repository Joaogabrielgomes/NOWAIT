import { useState } from 'react'
import { cepService, extrairMensagemErro } from '../services/api'

export default function EstabelecimentoForm({ inicial, aoSalvar, salvando }) {
  const [nome, setNome] = useState(inicial?.nome || '')
  const [cep, setCep] = useState(inicial?.endereco?.cep || '')
  const [numero, setNumero] = useState(inicial?.endereco?.numero || '')
  const [complemento, setComplemento] = useState(inicial?.endereco?.complemento || '')
  const [enderecoPreview, setEnderecoPreview] = useState(inicial?.endereco || null)
  const [buscandoCep, setBuscandoCep] = useState(false)
  const [erro, setErro] = useState('')

  const buscarCep = async () => {
    if (cep.replace(/\D/g, '').length !== 8) return
    setBuscandoCep(true)
    setErro('')
    try {
      const { data } = await cepService.buscar(cep)
      setEnderecoPreview(data)
    } catch (e) {
      setEnderecoPreview(null)
      setErro(extrairMensagemErro(e, 'CEP não encontrado'))
    } finally {
      setBuscandoCep(false)
    }
  }

  const submeter = async (e) => {
    e.preventDefault()
    setErro('')
    try {
      await aoSalvar({ nome, cep, numero, complemento })
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível salvar o estabelecimento'))
    }
  }

  return (
    <form onSubmit={submeter}>
      {erro && <div className="error-box">{erro}</div>}

      <div className="form-group">
        <label>Nome do estabelecimento</label>
        <input value={nome} onChange={(e) => setNome(e.target.value)} required maxLength={150} />
      </div>

      <div className="form-row">
        <div className="form-group">
          <label>CEP</label>
          <input
            value={cep}
            onChange={(e) => setCep(e.target.value)}
            onBlur={buscarCep}
            placeholder="00000-000"
            required
          />
        </div>
        <div className="form-group">
          <label>Número</label>
          <input value={numero} onChange={(e) => setNumero(e.target.value)} required maxLength={10} />
        </div>
      </div>

      <div className="form-group">
        <label>Complemento</label>
        <input value={complemento} onChange={(e) => setComplemento(e.target.value)} maxLength={100} />
      </div>

      {buscandoCep && <p className="text-muted">Buscando endereço...</p>}
      {enderecoPreview && !buscandoCep && (
        <p className="text-muted">
          {enderecoPreview.logradouro}, {enderecoPreview.bairro} — {enderecoPreview.cidade}/{enderecoPreview.estado}
        </p>
      )}

      <button type="submit" disabled={salvando}>
        {salvando ? 'Salvando...' : 'Salvar'}
      </button>
    </form>
  )
}
