export default function ConfirmModal({
  titulo = 'Confirmar ação',
  mensagem,
  textoConfirmar = 'Confirmar',
  textoCancelar = 'Voltar',
  confirmando = false,
  onConfirmar,
  onCancelar,
}) {
  return (
    <div className="modal-overlay" onClick={onCancelar}>
      <div className="modal-content" style={{ maxWidth: 420 }} onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onCancelar} aria-label="Fechar">×</button>
        <div className="modal-body" style={{ whiteSpace: 'normal' }}>
          <h2>{titulo}</h2>
          <p>{mensagem}</p>
        </div>
        <div className="btn-row" style={{ padding: '0 28px 24px', justifyContent: 'flex-end' }}>
          <button className="secondary small" onClick={onCancelar} disabled={confirmando}>
            {textoCancelar}
          </button>
          <button className="danger small" onClick={onConfirmar} disabled={confirmando}>
            {confirmando ? 'Confirmando...' : textoConfirmar}
          </button>
        </div>
      </div>
    </div>
  )
}
