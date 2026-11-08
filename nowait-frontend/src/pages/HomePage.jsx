import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { estabelecimentoService, favoritoService, filaService, avaliacaoService } from '../services/api'
import { useAuth } from '../context/AuthContext'
import StarRating from '../components/StarRating'
import Paginacao from '../components/Paginacao'

function itensPorPagina() {
  const w = window.innerWidth
  if (w < 640) return 4
  if (w < 960) return 6
  if (w < 1280) return 9
  return 12
}

function usePaginaResponsiva() {
  const [itens, setItens] = useState(itensPorPagina())
  useEffect(() => {
    let frame
    const aoRedimensionar = () => {
      cancelAnimationFrame(frame)
      frame = requestAnimationFrame(() => setItens(itensPorPagina()))
    }
    window.addEventListener('resize', aoRedimensionar)
    return () => window.removeEventListener('resize', aoRedimensionar)
  }, [])
  return itens
}

export default function HomePage() {
  const { usuario } = useAuth()
  const ehCliente = usuario?.role === 'CLIENTE'

  const [estabelecimentos, setEstabelecimentos] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [query, setQuery] = useState('')
  const [buscando, setBuscando] = useState(false)
  const [pagina, setPagina] = useState(1)
  const [favoritoIds, setFavoritoIds] = useState(new Set())
  const [apenasFavoritos, setApenasFavoritos] = useState(false)
  const [resumos, setResumos] = useState({})
  const [avaliacoesResumo, setAvaliacoesResumo] = useState({})
  const timerRef = useRef(null)
  const itensPagina = usePaginaResponsiva()

  useEffect(() => {
    if (!ehCliente) {
      setFavoritoIds(new Set())
      return
    }
    favoritoService.listarIds()
      .then(({ data }) => setFavoritoIds(new Set(data)))
      .catch(() => {})
  }, [ehCliente])

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

  const carregarTodos = () => {
    setCarregando(true)
    estabelecimentoService.listarTodos()
      .then(({ data }) => setEstabelecimentos(data))
      .finally(() => setCarregando(false))
  }

  useEffect(() => { carregarTodos() }, [])

  useEffect(() => {
    const termo = query.trim()

    if (!termo) {
      clearTimeout(timerRef.current)
      carregarTodos()
      return
    }

    clearTimeout(timerRef.current)
    timerRef.current = setTimeout(async () => {
      setBuscando(true)
      try {
        const { data } = await estabelecimentoService.buscarPorNome(termo)
        setEstabelecimentos(data || [])
      } catch {
        setEstabelecimentos([])
      } finally {
        setBuscando(false)
        setCarregando(false)
      }
    }, 350)

    return () => clearTimeout(timerRef.current)
  }, [query])

  const abertos = estabelecimentos.filter((est) => est.aberto)
  const listaFiltrada = apenasFavoritos ? abertos.filter((est) => favoritoIds.has(est.id)) : abertos

  const totalPaginas = Math.max(1, Math.ceil(listaFiltrada.length / itensPagina))
  const paginaAtual = Math.min(pagina, totalPaginas)

  useEffect(() => {
    setPagina(1)
  }, [query, itensPagina, estabelecimentos.length, apenasFavoritos])

  const visiveis = listaFiltrada.slice((paginaAtual - 1) * itensPagina, paginaAtual * itensPagina)

  useEffect(() => {
    const faltantes = visiveis.filter((est) => !(est.id in resumos)).map((est) => est.id)
    if (faltantes.length === 0) return

    let cancelado = false
    Promise.all(
      faltantes.map((id) =>
        filaService.resumoPublico(id)
          .then(({ data }) => [id, data])
          .catch(() => [id, null])
      )
    ).then((pares) => {
      if (cancelado) return
      setResumos((prev) => {
        const novo = { ...prev }
        pares.forEach(([id, data]) => { novo[id] = data })
        return novo
      })
    })

    return () => { cancelado = true }
  }, [visiveis.map((est) => est.id).join(',')])

  useEffect(() => {
    const faltantes = visiveis.filter((est) => !(est.id in avaliacoesResumo)).map((est) => est.id)
    if (faltantes.length === 0) return

    let cancelado = false
    Promise.all(
      faltantes.map((id) =>
        avaliacaoService.resumoPublico(id)
          .then(({ data }) => [id, data])
          .catch(() => [id, null])
      )
    ).then((pares) => {
      if (cancelado) return
      setAvaliacoesResumo((prev) => {
        const novo = { ...prev }
        pares.forEach(([id, data]) => { novo[id] = data })
        return novo
      })
    })

    return () => { cancelado = true }
  }, [visiveis.map((est) => est.id).join(',')])

  return (
    <div className="container">
      <h2>Estabelecimentos</h2>

      <div className="btn-row" style={{ alignItems: 'flex-end', flexWrap: 'wrap' }}>
        <div className="form-group" style={{ maxWidth: 360, flex: 1, minWidth: 220 }}>
          <input
            type="text"
            placeholder="🔍 Buscar estabelecimento..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            autoComplete="off"
          />
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

      {(carregando || buscando) && <p className="text-muted">Carregando...</p>}

      {!carregando && !buscando && listaFiltrada.length === 0 && (
        <div className="empty-state">
          {apenasFavoritos
            ? 'Você ainda não favoritou nenhum estabelecimento.'
            : query.trim()
              ? 'Nenhum estabelecimento encontrado.'
              : 'Nenhum estabelecimento aberto no momento.'}
        </div>
      )}

      {!carregando && !buscando && listaFiltrada.length > 0 && (
        <>
          <div className="grid">
            {visiveis.map((est) => {
              const resumo = resumos[est.id]
              const avaliacaoResumo = avaliacoesResumo[est.id]
              return (
                <Link
                  to={`/estabelecimentos/${est.id}`}
                  key={est.id}
                  className="card"
                  style={{ display: 'block', position: 'relative' }}
                >
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

                  <h3 style={{ paddingRight: ehCliente ? 28 : 0 }}>{est.nome}</h3>
                  <p className="text-muted">
                    {est.endereco.bairro} — {est.endereco.cidade}/{est.endereco.estado}
                  </p>
                  <span className="badge CHAMADO">Aberto</span>

                  {avaliacaoResumo?.totalAvaliacoes > 0 && (
                    <div className="btn-row" style={{ alignItems: 'center', marginTop: 6 }}>
                      <StarRating valor={avaliacaoResumo.mediaNota} somenteLeitura tamanho="0.85rem" />
                      <span className="text-muted" style={{ fontSize: '0.78rem' }}>
                        {avaliacaoResumo.mediaNota.toFixed(1)} ({avaliacaoResumo.totalAvaliacoes})
                      </span>
                    </div>
                  )}

                  <p className="text-muted" style={{ fontSize: '0.8rem', margin: '8px 0 0' }}>
                    {!resumo
                      ? ' '
                      : resumo.tempoEsperaEstimadoMinutos != null
                        ? `⏱ ~${resumo.tempoEsperaEstimadoMinutos} min de espera`
                        : '⏱ Sem espera no momento'}
                  </p>
                </Link>
              )
            })}
          </div>

          <Paginacao paginaAtual={paginaAtual} totalPaginas={totalPaginas} aoMudar={setPagina} />
        </>
      )}
    </div>
  )
}
