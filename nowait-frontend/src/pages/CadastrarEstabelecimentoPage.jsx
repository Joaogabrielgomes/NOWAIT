import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { estabelecimentoService } from '../services/api'
import EstabelecimentoForm from '../components/EstabelecimentoForm'

export default function CadastrarEstabelecimentoPage() {
  const [salvando, setSalvando] = useState(false)
  const navigate = useNavigate()

  const salvar = async (dto) => {
    setSalvando(true)
    try {
      await estabelecimentoService.criar(dto)
      navigate('/painel')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="container">
      <h2>Cadastrar meu estabelecimento</h2>
      <p className="text-muted">
        Informe os dados do seu restaurante. O endereço é preenchido automaticamente a partir do CEP.
      </p>
      <div className="card" style={{ maxWidth: 480 }}>
        <EstabelecimentoForm aoSalvar={salvar} salvando={salvando} />
      </div>
    </div>
  )
}
