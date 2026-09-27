import { useEffect, useState } from 'react'
import { filaService, extrairMensagemErro } from '../services/api'
import { conectarFila } from '../services/ws'

export default function PainelFila({ estabelecimentoId }) {
  const [fila, setFila] = useState([])
  const [erro, setErro] = useState('')
  const [acaoEmCurso, setAcaoEmCurso] = useState(null)

  useEffect(() => {
    filaService.listar(estabelecimentoId).then(({ data }) => setFila(data))
    const cleanup = conectarFila(estabelecimentoId, setFila)
    return cleanup
  }, [estabelecimentoId])

  const executar = async (acao, id) => {
    setErro('')
    setAcaoEmCurso(id)
    try {
      if (acao === 'chamar') await filaService.chamar(id)
      if (acao === 'atender') await filaService.atender(id)
      if (acao === 'cancelar') await filaService.cancelar(id)
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível executar a ação'))
    } finally {
      setAcaoEmCurso(null)
    }
  }

  const aguardando = fila.filter((f) => f.status === 'AGUARDANDO')
  const chamados = fila.filter((f) => f.status === 'CHAMADO')

  return (
    <div>
      {erro && <div className="error-box">{erro}</div>}

      {chamados.length > 0 && (
        <div className="card">
          <h3>Chamados</h3>
          {chamados.map((f) => (
            <div key={f.id} className="btn-row" style={{ justifyContent: 'space-between', marginBottom: 8 }}>
              <span>{f.clienteNome} — {f.quantidadePessoas} pessoa(s)</span>
              <div className="btn-row">
                <button className="small" onClick={() => executar('atender', f.id)} disabled={acaoEmCurso === f.id}>
                  Marcar atendido
                </button>
                <button className="secondary small" onClick={() => executar('cancelar', f.id)} disabled={acaoEmCurso === f.id}>
                  Cancelar
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="card">
        <h3>Aguardando ({aguardando.length})</h3>
        {aguardando.length === 0 && <p className="text-muted">Ninguém aguardando no momento.</p>}
        {aguardando.map((f) => (
          <div key={f.id} className="btn-row" style={{ justifyContent: 'space-between', marginBottom: 8 }}>
            <span>
              <strong>#{f.posicao}</strong> — {f.clienteNome} — {f.quantidadePessoas} pessoa(s)
            </span>
            <div className="btn-row">
              <button className="small" onClick={() => executar('chamar', f.id)} disabled={acaoEmCurso === f.id}>
                Chamar
              </button>
              <button className="secondary small" onClick={() => executar('cancelar', f.id)} disabled={acaoEmCurso === f.id}>
                Cancelar
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
