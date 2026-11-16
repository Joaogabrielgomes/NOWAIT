import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { estabelecimentoService, favoritoService, filaService, avaliacaoService } from '../services/api'
import { useAuth } from '../context/AuthContext'
import StarRating from '../components/StarRating'
import Paginacao from '../components/Paginacao'
import { CATEGORIAS_ESTABELECIMENTO, rotuloCategoria } from '../constants/categorias'
import { corAvatar } from '../utils/avatar'
import { iconeCategoria } from '../utils/iconeCategoria'

const PAGINA_VAZIA = { content: [], totalPages: 0, totalElements: 0 }

function tamanhoPagina() {
  const w = window.innerWidth
  if (w < 640) return 4
  if (w < 960) return 6
  if (w < 1280) return 9
  return 12
}

function useTamanhoResponsivo() {
  const [tamanho, setTamanho] = useState(tamanhoPagina())
  useEffect(() => {
    let frame
    const aoRedimensionar = () => {
      cancelAnimationFrame(frame)
      frame = requestAnimationFrame(() => setTamanho(tamanhoPagina()))
    }
    window.addEventListener('resize', aoRedimensionar)
    return () => window.removeEventListener('resize', aoRedimensionar)
  }, [])
  return tamanho
}

export default function HomePage() {
  const { usuario } = useAuth()
  const ehCliente = usuario?.role === 'CLIENTE'

  const [estabelecimentosPage, setEstabelecimentosPage] = useState(PAGINA_VAZIA)
  const [pagina, setPagina] = useState(0)
  const [query, setQuery] = useState('')
  const [cidade, setCidade] = useState('')
  const [bairro, setBairro] = useState('')
  const [categoria, setCategoria] = useState('')
  const [carregando, setCarregando] = useState(true)

  const [apenasFavoritos, setApenasFavoritos] = useState(false)
  const [favoritos, setFavoritos] = useState([])
  const [favoritoIds, setFavoritoIds] = useState(new Set())
  const [paginaFavoritos, setPaginaFavoritos] = useState(1)

  const [resumos, setResumos] = useState({})
  const [avaliacoesResumo, setAvaliacoesResumo] = useState({})
  const timerRef = useRef(null)
  const tamanho = useTamanhoResponsivo()

  useEffect(() => {
    if (!ehCliente) {
      setFavoritoIds(new Set())
      return
    }
    favoritoService.listarIds()
      .then(({ data }) => setFavoritoIds(new Set(data)))
      .catch(() => {})
  }, [ehCliente])

  useEffect(() => {
    if (!apenasFavoritos) return
    favoritoService.listarMeus()
      .then(({ data }) => setFavoritos(data || []))
      .catch(() => setFavoritos([]))
  }, [apenasFavoritos, favoritoIds])

  const alternarFavorito = async (e, id) => {
    e.preventDefault()
    e.stopPropagation()
    const jaFavoritado = favoritoIds.has(id)

    setFavoritoIds((prev) => {
      const novo = new Set(prev)
      jaFavoritado ? novo.delete(id) : novo.add(id)
      return novo
    })

    try {
      jaFavoritado ? await favoritoService.remover(id) : await favoritoService.adicionar(id)
    } catch {
      setFavoritoIds((prev) => {
        const novo = new Set(prev)
        jaFavoritado ? novo.add(id) : novo.delete(id)
        return novo
      })
    }
  }

  useEffect(() => {
    if (apenasFavoritos) return
    const termo = query.trim()
    const cidadeTermo = cidade.trim()
    const bairroTermo = bairro.trim()
    setCarregando(true)

    clearTimeout(timerRef.current)
    timerRef.current = setTimeout(() => {
      estabelecimentoService.listar({
        nome: termo || undefined,
        cidade: cidadeTermo || undefined,
        bairro: bairroTermo || undefined,
        categoria: categoria || undefined,
        pagina,
        tamanho,
      })
        .then(({ data }) => setEstabelecimentosPage(data))
        .catch(() => setEstabelecimentosPage(PAGINA_VAZIA))
        .finally(() => setCarregando(false))
    }, termo || cidadeTermo || bairroTermo ? 350 : 0)

    return () => clearTimeout(timerRef.current)
  }, [query, cidade, bairro, categoria, pagina, tamanho, apenasFavoritos])

  useEffect(() => { setPagina(0) }, [query, cidade, bairro, categoria, tamanho])
  useEffect(() => { setPaginaFavoritos(1) }, [tamanho])

  const totalPaginasFavoritos = Math.max(1, Math.ceil(favoritos.length / tamanho))
  const paginaFavoritosAtual = Math.min(paginaFavoritos, totalPaginasFavoritos)
  const favoritosVisiveis = favoritos.slice(
    (paginaFavoritosAtual - 1) * tamanho,
    paginaFavoritosAtual * tamanho
  )

  const visiveis = apenasFavoritos ? favoritosVisiveis : estabelecimentosPage.content
  const totalPaginas = apenasFavoritos ? totalPaginasFavoritos : Math.max(1, estabelecimentosPage.totalPages)
  const paginaAtualExibida = apenasFavoritos ? paginaFavoritosAtual : pagina + 1
  const semResultados = !carregando && visiveis.length === 0

  useEffect(() => {
    const faltantes = visiveis.filter((est) => !(est.id in resumos)).map((est) => est.id)
    if (faltantes.length === 0) return

    let cancelado = false
    filaService.resumoPublicoLote(faltantes)
      .then(({ data }) => {
        if (cancelado) return
        setResumos((prev) => ({ ...prev, ...data }))
      })
      .catch(() => {})

    return () => { cancelado = true }
  }, [visiveis.map((est) => est.id).join(',')])

  useEffect(() => {
    const faltantes = visiveis.filter((est) => !(est.id in avaliacoesResumo)).map((est) => est.id)
    if (faltantes.length === 0) return

    let cancelado = false
    avaliacaoService.resumoPublicoLote(faltantes)
      .then(({ data }) => {
        if (cancelado) return
        setAvaliacoesResumo((prev) => ({ ...prev, ...data }))
      })
      .catch(() => {})

    return () => { cancelado = true }
  }, [visiveis.map((est) => est.id).join(',')])

  const mudarPagina = (p) => {
    if (apenasFavoritos) setPaginaFavoritos(p)
    else setPagina(p - 1)
  }

  return (
    <div className="container">
      <h2>Estabelecimentos</h2>

      <div className="btn-row" style={{ alignItems: 'flex-end', flexWrap: 'wrap' }}>
        <div className="form-group" style={{ maxWidth: 360, flex: 2, minWidth: 220 }}>
          <input
            type="text"
            placeholder="🔍 Buscar estabelecimento..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            autoComplete="off"
            disabled={apenasFavoritos}
          />
        </div>

        <div className="form-group" style={{ maxWidth: 200, flex: 1, minWidth: 140 }}>
          <input
            type="text"
            placeholder="Cidade"
            value={cidade}
            onChange={(e) => setCidade(e.target.value)}
            autoComplete="off"
            disabled={apenasFavoritos}
          />
        </div>

        <div className="form-group" style={{ maxWidth: 200, flex: 1, minWidth: 140 }}>
          <input
            type="text"
            placeholder="Bairro"
            value={bairro}
            onChange={(e) => setBairro(e.target.value)}
            autoComplete="off"
            disabled={apenasFavoritos}
          />
        </div>

        <div className="form-group" style={{ maxWidth: 220, flex: 1, minWidth: 160 }}>
          <select
            value={categoria}
            onChange={(e) => setCategoria(e.target.value)}
            disabled={apenasFavoritos}
          >
            <option value="">Todas as categorias</option>
            {CATEGORIAS_ESTABELECIMENTO.map((c) => (
              <option key={c.valor} value={c.valor}>{c.rotulo}</option>
            ))}
          </select>
        </div>

        {ehCliente && (
          <button
            className={apenasFavoritos ? '' : 'secondary'}
            onClick={() => setApenasFavoritos((v) => !v)}
            style={{ marginBottom: 14 }}
          >
            {apenasFavoritos ? '★ Favoritos' : '☆ Favoritos'}
          </button>
        )}
      </div>

      {carregando && !apenasFavoritos && <p className="text-muted">Carregando...</p>}

      {semResultados && (
        <div className="empty-state">
          {apenasFavoritos
            ? 'Você ainda não favoritou nenhum estabelecimento.'
            : query.trim() || cidade.trim() || bairro.trim() || categoria
              ? 'Nenhum estabelecimento encontrado com esses filtros.'
              : 'Nenhum estabelecimento aberto no momento.'}
        </div>
      )}

      {!semResultados && visiveis.length > 0 && (
        <>
          <div className="grid">
            {visiveis.map((est) => {
              const resumo = resumos[est.id]
              const avaliacaoResumo = avaliacoesResumo[est.id]
              const IconeCategoria = iconeCategoria(est.categoria)
              return (
                <Link
                  to={`/estabelecimentos/${est.id}`}
                  key={est.id}
                  className="estabelecimento-card"
                >
                  {est.logoUrl ? (
                    <img src={est.logoUrl} alt="" className="estabelecimento-avatar" />
                  ) : (
                    <div className="estabelecimento-avatar" style={{ background: corAvatar(est.nome) }}>
                      <IconeCategoria size={28} strokeWidth={2} />
                    </div>
                  )}

                  <div className="estabelecimento-info">
                    <div className="estabelecimento-topo">
                      <h3>{est.nome}</h3>
                      {ehCliente && (
                        <button
                          className={`favorito-btn ${favoritoIds.has(est.id) ? 'ativo' : ''}`}
                          onClick={(e) => alternarFavorito(e, est.id)}
                          aria-label={favoritoIds.has(est.id) ? 'Remover dos favoritos' : 'Adicionar aos favoritos'}
                          title={favoritoIds.has(est.id) ? 'Remover dos favoritos' : 'Adicionar aos favoritos'}
                        >
                          {favoritoIds.has(est.id) ? '★' : '☆'}
                        </button>
                      )}
                    </div>

                    <p className="estabelecimento-meta">
                      {avaliacaoResumo?.totalAvaliacoes > 0 ? (
                        <>
                          <StarRating valor={avaliacaoResumo.mediaNota} somenteLeitura tamanho="0.8rem" />
                          {avaliacaoResumo.mediaNota.toFixed(1)}
                          <span className="separador">•</span>
                        </>
                      ) : null}
                      {rotuloCategoria(est.categoria)}
                      <span className="separador">•</span>
                      {est.endereco.bairro}
                    </p>

                    <p className="estabelecimento-meta">
                      {est.horarioAbertura?.slice(0, 5)} às {est.horarioFechamento?.slice(0, 5)}
                      <span className="separador">•</span>
                      {String(est.mesasDisponiveis).padStart(2, '0')}/{String(est.totalMesas).padStart(2, '0')} mesas disponíveis
                    </p>

                    <p className="estabelecimento-footer">
                      <span>
                        {!resumo
                          ? ' '
                          : resumo.tempoEsperaEstimadoMinutos != null
                            ? `⏱ ~${resumo.tempoEsperaEstimadoMinutos} min de espera`
                            : '⏱ Sem espera no momento'}
                      </span>
                      <span className="badge CHAMADO">Aberto</span>
                    </p>
                  </div>
                </Link>
              )
            })}
          </div>

          <Paginacao paginaAtual={paginaAtualExibida} totalPaginas={totalPaginas} aoMudar={mudarPagina} />
        </>
      )}
    </div>
  )
}
