import { useState } from 'react'
import { denunciaService, extrairMensagemErro } from '../services/api'
import MotivoModal from './MotivoModal'

export default function DenunciaButton({
  tipoAlvo,
  alvoId,
  rotulo = 'Denunciar',
  titulo = 'Denunciar',
  mensagem,
  className = 'secondary small',
}) {
  const [aberto, setAberto] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [enviada, setEnviada] = useState(false)
  const [erro, setErro] = useState('')

  const enviar = async (motivo) => {
    setEnviando(true)
    setErro('')
    try {
      await denunciaService.criar({ tipoAlvo, alvoId, motivo })
      setAberto(false)
      setEnviada(true)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível enviar a denúncia'))
    } finally {
      setEnviando(false)
    }
  }

  if (enviada) {
    return <span className="text-muted" style={{ fontSize: '0.8rem' }}>Denúncia enviada</span>
  }

  return (
    <>
      <button type="button" className={className} onClick={() => setAberto(true)}>
        {rotulo}
      </button>

      {aberto && (
        <MotivoModal
          titulo={titulo}
          mensagem={mensagem}
          placeholder="Explique o motivo da denúncia..."
          textoConfirmar="Enviar denúncia"
          maxLength={500}
          confirmando={enviando}
          onConfirmar={enviar}
          onCancelar={() => setAberto(false)}
        />
      )}

      {erro && (
        <div className="error-box" style={{ marginTop: 8 }}>
          {erro}
        </div>
      )}
    </>
  )
}
