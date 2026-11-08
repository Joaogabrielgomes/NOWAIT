import { useState } from 'react'

export default function MotivoModal({
  titulo = 'Informar motivo',
  mensagem,
  placeholder = 'Explique o motivo...',
  textoConfirmar = 'Confirmar',
  textoCancelar = 'Voltar',
  confirmando = false,
  onConfirmar,
  onCancelar,
}) {
  const [motivo, setMotivo] = useState('')
  const [erro, setErro] = useState('')

  const confirmar = () => {
    if (!motivo.trim()) {
      setErro('Informe o motivo.')
      return
    }
    onConfirmar(motivo.trim())
  }

  return (
    <div className="modal-overlay" onClick={onCancelar}>
      <div className="modal-content" style={{ maxWidth: 420 }} onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onCancelar} aria-label="Fechar">×</button>
        <div className="modal-body" style={{ whiteSpace: 'normal' }}>
          <h2>{titulo}</h2>
          {mensagem && <p>{mensagem}</p>}
          <div className="form-group">
            <label>Motivo</label>
            <textarea
              value={motivo}
              onChange={(e) => { setMotivo(e.target.value); setErro('') }}
              placeholder={placeholder}
              rows={3}
              maxLength={255}
            />
            {erro && <span style={{ color: 'var(--danger)', fontSize: '0.78rem' }}>{erro}</span>}
          </div>
        </div>
        <div className="btn-row" style={{ padding: '0 28px 24px', justifyContent: 'flex-end' }}>
          <button className="secondary small" onClick={onCancelar} disabled={confirmando}>
            {textoCancelar}
          </button>
          <button className="danger small" onClick={confirmar} disabled={confirmando}>
            {confirmando ? 'Enviando...' : textoConfirmar}
          </button>
        </div>
      </div>
    </div>
  )
}
