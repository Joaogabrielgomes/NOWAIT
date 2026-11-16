import { useState } from 'react'
import StarRating from './StarRating'

export default function AvaliacaoModal({
  titulo = 'Avaliar estabelecimento',
  mensagem,
  confirmando = false,
  onConfirmar,
  onCancelar,
  avaliacaoInicial,
}) {
  const [nota, setNota] = useState(avaliacaoInicial?.nota ?? 0)
  const [comentario, setComentario] = useState(avaliacaoInicial?.comentario ?? '')
  const [erro, setErro] = useState('')

  const confirmar = () => {
    if (nota < 1) {
      setErro('Selecione uma nota de 1 a 5 estrelas.')
      return
    }
    onConfirmar(nota, comentario.trim() || undefined)
  }

  return (
    <div className="modal-overlay" onClick={onCancelar}>
      <div className="modal-content" style={{ maxWidth: 420 }} onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onCancelar} aria-label="Fechar">×</button>
        <div className="modal-body" style={{ whiteSpace: 'normal' }}>
          <h2>{titulo}</h2>
          {mensagem && <p>{mensagem}</p>}

          <div className="form-group">
            <label>Nota</label>
            <div>
              <StarRating valor={nota} tamanho="1.8rem" onChange={(n) => { setNota(n); setErro('') }} />
            </div>
            {erro && <span style={{ color: 'var(--danger)', fontSize: '0.78rem' }}>{erro}</span>}
          </div>

          <div className="form-group">
            <label>Comentário (opcional)</label>
            <textarea
              value={comentario}
              onChange={(e) => setComentario(e.target.value)}
              placeholder="Conte como foi sua experiência..."
              rows={3}
              maxLength={500}
            />
          </div>
        </div>
        <div className="btn-row" style={{ padding: '0 28px 24px', justifyContent: 'flex-end' }}>
          <button className="secondary small" onClick={onCancelar} disabled={confirmando}>
            Voltar
          </button>
          <button className="small" onClick={confirmar} disabled={confirmando}>
            {avaliacaoInicial
              ? (confirmando ? 'Salvando...' : 'Salvar alterações')
              : (confirmando ? 'Enviando...' : 'Enviar avaliação')}
          </button>
        </div>
      </div>
    </div>
  )
}
