import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { estabelecimentoService } from '../services/api'
import PainelMesas from '../components/PainelMesas'
import PainelFila from '../components/PainelFila'
import PainelAgendamentos from '../components/PainelAgendamentos'
import EstabelecimentoForm from '../components/EstabelecimentoForm'
import UploadImagemEstabelecimento from '../components/UploadImagemEstabelecimento'

export default function PainelEstabelecimentoPage() {
  const [estabelecimento, setEstabelecimento] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [semEstabelecimento, setSemEstabelecimento] = useState(false)
  const [aba, setAba] = useState('fila')
  const [editando, setEditando] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [alternandoAberto, setAlternandoAberto] = useState(false)

  useEffect(() => {
    estabelecimentoService.meuEstabelecimento()
      .then(({ data }) => setEstabelecimento(data))
      .catch(() => setSemEstabelecimento(true))
      .finally(() => setCarregando(false))
  }, [])

  const salvarEdicao = async (dto) => {
    setSalvando(true)
    try {
      const { data } = await estabelecimentoService.atualizar(estabelecimento.id, dto)
      setEstabelecimento(data)
      setEditando(false)
    } finally {
      setSalvando(false)
    }
  }

  const alternarAberto = async () => {
    setAlternandoAberto(true)
    try {
      const { data } = await estabelecimentoService.alternarAberto(estabelecimento.id)
      setEstabelecimento(data)
    } finally {
      setAlternandoAberto(false)
    }
  }

  if (carregando) return <div className="container"><p className="text-muted">Carregando...</p></div>
  if (semEstabelecimento) return <Navigate to="/painel/cadastro" replace />

  if (estabelecimento.statusAprovacao !== 'APROVADO') {
    return (
      <div className="container">
        <h2>{estabelecimento.nome}</h2>
        <div className="info-box">
          {estabelecimento.statusAprovacao === 'REJEITADO'
            ? 'Seu estabelecimento foi rejeitado pelo administrador. Entre em contato com o suporte para mais informações.'
            : 'Seu estabelecimento está aguardando aprovação do administrador. Você poderá configurar mesas, fila e agendamentos assim que for aprovado.'}
        </div>
      </div>
    )
  }

  return (
    <div className="container">
      {!editando && (
        <>
          <div className="btn-row" style={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <h2 style={{ marginBottom: 4 }}>{estabelecimento.nome}</h2>
              <p className="text-muted" style={{ marginTop: 0 }}>
                {estabelecimento.endereco.bairro} — {estabelecimento.endereco.cidade}/{estabelecimento.endereco.estado}
                {' · '}{estabelecimento.horarioAbertura?.slice(0, 5)} às {estabelecimento.horarioFechamento?.slice(0, 5)}
              </p>
            </div>
            <div className="btn-row">
              <button
                className={estabelecimento.aberto ? 'danger small' : 'small'}
                onClick={alternarAberto}
                disabled={alternandoAberto}
              >
                {estabelecimento.aberto ? 'Fechar estabelecimento' : 'Abrir estabelecimento'}
              </button>
              <button className="secondary small" onClick={() => setEditando(true)}>Editar dados</button>
            </div>
          </div>

          <div className="btn-row mb-16">
            <button className={aba === 'fila' ? '' : 'secondary'} onClick={() => setAba('fila')}>Fila de espera</button>
            <button className={aba === 'mesas' ? '' : 'secondary'} onClick={() => setAba('mesas')}>Mesas</button>
            <button className={aba === 'agendamentos' ? '' : 'secondary'} onClick={() => setAba('agendamentos')}>Agendamentos</button>
          </div>

          {aba === 'fila' && <PainelFila estabelecimentoId={estabelecimento.id} />}
          {aba === 'mesas' && <PainelMesas estabelecimentoId={estabelecimento.id} />}
          {aba === 'agendamentos' && <PainelAgendamentos estabelecimentoId={estabelecimento.id} />}
        </>
      )}

      {editando && (
        <>
          <h2>Editar estabelecimento</h2>

          <div className="card btn-row mb-16" style={{ gap: 32, flexWrap: 'wrap' }}>
            <UploadImagemEstabelecimento
              tipo="logo"
              urlAtual={estabelecimento.logoUrl}
              aoEnviar={async (arquivo) => {
                const { data } = await estabelecimentoService.enviarLogo(estabelecimento.id, arquivo)
                setEstabelecimento(data)
              }}
              aoRemover={async () => {
                const { data } = await estabelecimentoService.removerLogo(estabelecimento.id)
                setEstabelecimento(data)
              }}
            />
            <UploadImagemEstabelecimento
              tipo="banner"
              urlAtual={estabelecimento.bannerUrl}
              aoEnviar={async (arquivo) => {
                const { data } = await estabelecimentoService.enviarBanner(estabelecimento.id, arquivo)
                setEstabelecimento(data)
              }}
              aoRemover={async () => {
                const { data } = await estabelecimentoService.removerBanner(estabelecimento.id)
                setEstabelecimento(data)
              }}
            />
          </div>

          <div className="card" style={{ maxWidth: 480 }}>
            <EstabelecimentoForm
              inicial={estabelecimento}
              aoSalvar={salvarEdicao}
              aoCancelar={() => setEditando(false)}
              salvando={salvando}
            />
          </div>
        </>
      )}
    </div>
  )
}
