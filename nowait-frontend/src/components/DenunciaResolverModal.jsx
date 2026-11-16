import { useState } from 'react'

const ROTULOS_TIPO_ALVO = {
  ESTABELECIMENTO: 'Estabelecimento',
  AVALIACAO: 'Avaliação',
  USUARIO: 'Usuário',
}

export default function DenunciaResolverModal({ denuncia, resolvendo, onResolver, onCancelar }) {
  const [banirUsuarioAlvo, setBanirUsuarioAlvo] = useState(false)
  const [observacaoAdmin, setObservacaoAdmin] = useState('')

  const resolver = (aprovar) => {
    onResolver({
      aprovar,
      banirUsuarioAlvo: aprovar ? banirUsuarioAlvo : false,
      observacaoAdmin: observacaoAdmin.trim() || undefined,
    })
  }

  return (
    <div className="modal-overlay" onClick={onCancelar}>
      <div className="modal-content" style={{ maxWidth: 480 }} onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onCancelar} aria-label="Fechar">×</button>
        <div className="modal-body" style={{ whiteSpace: 'normal' }}>
          <h2>Resolver denúncia</h2>

          <p className="text-muted" style={{ marginBottom: 4 }}>
            {ROTULOS_TIPO_ALVO[denuncia.tipoAlvo] || denuncia.tipoAlvo}: <strong>{denuncia.alvoDescricao}</strong>
          </p>
          <p className="text-muted" style={{ marginBottom: 4 }}>
            Denunciado por: {denuncia.denuncianteNome}
          </p>
          <p style={{ marginBottom: 16 }}>{denuncia.motivo}</p>

          <div className="form-group">
            <label>Observação (opcional)</label>
            <textarea
              value={observacaoAdmin}
              onChange={(e) => setObservacaoAdmin(e.target.value)}
              placeholder="Registre o motivo da decisão, se quiser..."
              rows={3}
              maxLength={500}
            />
          </div>

          {denuncia.usuarioAlvoId && (
            <label className="btn-row" style={{ alignItems: 'center', gap: 8, cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={banirUsuarioAlvo}
                onChange={(e) => setBanirUsuarioAlvo(e.target.checked)}
              />
              Banir o usuário responsável ao aprovar esta denúncia
            </label>
          )}
        </div>

        <div className="btn-row" style={{ padding: '0 28px 24px', justifyContent: 'flex-end' }}>
          <button className="secondary small" onClick={onCancelar} disabled={resolvendo}>
            Voltar
          </button>
          <button className="danger small" onClick={() => resolver(false)} disabled={resolvendo}>
            {resolvendo ? 'Enviando...' : 'Rejeitar'}
          </button>
          <button className="small" onClick={() => resolver(true)} disabled={resolvendo}>
            {resolvendo ? 'Enviando...' : 'Aprovar'}
          </button>
        </div>
      </div>
    </div>
  )
}
