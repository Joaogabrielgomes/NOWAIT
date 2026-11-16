import { useRef, useState } from 'react'
import {
  LIMITES_IMAGEM_ESTABELECIMENTO,
  TAMANHO_MAXIMO_IMAGEM_BYTES,
  TIPOS_IMAGEM_PERMITIDOS,
} from '../constants/imagemEstabelecimento'
import { extrairMensagemErro } from '../services/api'

function validarDimensoes(arquivo, limite) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(arquivo)
    const imagem = new Image()

    imagem.onload = () => {
      URL.revokeObjectURL(url)
      if (imagem.naturalWidth < limite.larguraMinima || imagem.naturalHeight < limite.alturaMinima) {
        reject(new Error(
          `A imagem deve ter no mínimo ${limite.larguraMinima}x${limite.alturaMinima} pixels ` +
          `(enviada: ${imagem.naturalWidth}x${imagem.naturalHeight})`))
        return
      }
      resolve()
    }

    imagem.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('Não foi possível ler a imagem selecionada'))
    }

    imagem.src = url
  })
}

export default function UploadImagemEstabelecimento({ tipo, urlAtual, aoEnviar, aoRemover, className }) {
  const limite = LIMITES_IMAGEM_ESTABELECIMENTO[tipo]
  const inputRef = useRef(null)
  const [enviando, setEnviando] = useState(false)
  const [removendo, setRemovendo] = useState(false)
  const [erro, setErro] = useState('')

  const selecionarArquivo = async (e) => {
    const arquivo = e.target.files?.[0]
    e.target.value = ''
    if (!arquivo) return

    setErro('')

    if (arquivo.size > TAMANHO_MAXIMO_IMAGEM_BYTES) {
      setErro('A imagem deve ter no máximo 5MB')
      return
    }
    if (!TIPOS_IMAGEM_PERMITIDOS.includes(arquivo.type)) {
      setErro('Formato não suportado. Envie JPEG, PNG ou WEBP')
      return
    }

    try {
      await validarDimensoes(arquivo, limite)
    } catch (e) {
      setErro(e.message)
      return
    }

    setEnviando(true)
    try {
      await aoEnviar(arquivo)
    } catch (e) {
      setErro(extrairMensagemErro(e, `Não foi possível enviar o ${limite.rotulo}`))
    } finally {
      setEnviando(false)
    }
  }

  const remover = async () => {
    setErro('')
    setRemovendo(true)
    try {
      await aoRemover()
    } catch (e) {
      setErro(extrairMensagemErro(e, `Não foi possível remover o ${limite.rotulo}`))
    } finally {
      setRemovendo(false)
    }
  }

  return (
    <div className={`upload-imagem-wrapper ${className || ''}`}>
      {urlAtual ? (
        <img src={urlAtual} alt={`${limite.rotulo} do estabelecimento`} className={`upload-imagem-preview upload-imagem-preview-${tipo}`} />
      ) : (
        <div className={`upload-imagem-vazio upload-imagem-preview-${tipo}`}>Sem {limite.rotulo}</div>
      )}

      <div className="btn-row">
        <button
          type="button"
          className="secondary small upload-imagem-botao"
          onClick={() => inputRef.current?.click()}
          disabled={enviando || removendo}
        >
          {enviando ? 'Enviando...' : `Alterar ${limite.rotulo}`}
        </button>

        {urlAtual && aoRemover && (
          <button
            type="button"
            className="danger small"
            onClick={remover}
            disabled={enviando || removendo}
          >
            {removendo ? 'Removendo...' : 'Remover'}
          </button>
        )}
      </div>

      <input
        ref={inputRef}
        type="file"
        accept={TIPOS_IMAGEM_PERMITIDOS.join(',')}
        onChange={selecionarArquivo}
        hidden
      />

      {erro && <p className="upload-imagem-erro">{erro}</p>}
    </div>
  )
}
