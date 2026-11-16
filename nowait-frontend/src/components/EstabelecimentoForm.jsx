import { useState } from 'react'
import { cepService, extrairMensagemErro } from '../services/api'
import { CATEGORIAS_ESTABELECIMENTO } from '../constants/categorias'
import CampoAjuda from './CampoAjuda'

export default function EstabelecimentoForm({ inicial, aoSalvar, aoCancelar, salvando }) {
  const [nome, setNome] = useState(inicial?.nome || '')
  const [categoria, setCategoria] = useState(inicial?.categoria || '')
  const [telefone, setTelefone] = useState(inicial?.telefone || '')
  const [cep, setCep] = useState(inicial?.endereco?.cep || '')
  const [numero, setNumero] = useState(inicial?.endereco?.numero || '')
  const [complemento, setComplemento] = useState(inicial?.endereco?.complemento || '')
  const [toleranciaPadraoMinutos, setToleranciaPadraoMinutos] = useState(inicial?.toleranciaPadraoMinutos ?? 15)
  const [tempoMedioAtendimentoMinutos, setTempoMedioAtendimentoMinutos] = useState(inicial?.tempoMedioAtendimentoMinutos ?? 15)
  const [horarioAbertura, setHorarioAbertura] = useState(inicial?.horarioAbertura?.slice(0, 5) ?? '08:00')
  const [horarioFechamento, setHorarioFechamento] = useState(inicial?.horarioFechamento?.slice(0, 5) ?? '22:00')
  const [antecedenciaMaximaDias, setAntecedenciaMaximaDias] = useState(inicial?.antecedenciaMaximaDias ?? 7)
  const [antecedenciaReservaMesaMinutos, setAntecedenciaReservaMesaMinutos] =
    useState(inicial?.antecedenciaReservaMesaMinutos ?? 15)
  const [antecedenciaMinimaMinutos, setAntecedenciaMinimaMinutos] =
    useState(inicial?.antecedenciaMinimaMinutos ?? 30)
  const [enderecoPreview, setEnderecoPreview] = useState(inicial?.endereco || null)
  const [buscandoCep, setBuscandoCep] = useState(false)
  const [erro, setErro] = useState('')

  const podeEditarConfiguracoes = Boolean(inicial) && inicial.statusAprovacao === 'APROVADO'

  const buscarCep = async () => {
    if (cep.replace(/\D/g, '').length !== 8) return
    setBuscandoCep(true)
    setErro('')
    try {
      const { data } = await cepService.buscar(cep)
      setEnderecoPreview(data)
    } catch (e) {
      setEnderecoPreview(null)
      setErro(extrairMensagemErro(e, 'CEP não encontrado'))
    } finally {
      setBuscandoCep(false)
    }
  }

  const submeter = async (e) => {
    e.preventDefault()
    setErro('')
    try {
      await aoSalvar({
        nome, categoria, telefone, cep, numero, complemento,
        ...(podeEditarConfiguracoes ? {
          toleranciaPadraoMinutos: Number(toleranciaPadraoMinutos),
          tempoMedioAtendimentoMinutos: Number(tempoMedioAtendimentoMinutos),
          horarioAbertura, horarioFechamento,
          antecedenciaMaximaDias: Number(antecedenciaMaximaDias),
          antecedenciaReservaMesaMinutos: Number(antecedenciaReservaMesaMinutos),
          antecedenciaMinimaMinutos: Number(antecedenciaMinimaMinutos),
        } : {}),
      })
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível salvar o estabelecimento'))
    }
  }

  return (
    <form onSubmit={submeter} className="form-titulos-destaque">
      {erro && <div className="error-box">{erro}</div>}

      <div className="form-group">
        <label>Nome do estabelecimento</label>
        <input value={nome} onChange={(e) => setNome(e.target.value)} required maxLength={150} />
      </div>

      <div className="form-group">
        <label>Categoria</label>
        <select value={categoria} onChange={(e) => setCategoria(e.target.value)} required>
          <option value="" disabled>Selecione uma categoria</option>
          {CATEGORIAS_ESTABELECIMENTO.map((c) => (
            <option key={c.valor} value={c.valor}>{c.rotulo}</option>
          ))}
        </select>
      </div>

      <div className="form-group">
        <label>Telefone</label>
        <input
          value={telefone}
          onChange={(e) => setTelefone(e.target.value)}
          placeholder="(11) 98765-4321"
          required
          maxLength={20}
        />
      </div>

      <div className="form-row">
        <div className="form-group">
          <label>CEP</label>
          <input
            value={cep}
            onChange={(e) => setCep(e.target.value)}
            onBlur={buscarCep}
            placeholder="00000-000"
            required
          />
        </div>
        <div className="form-group">
          <label>Número</label>
          <input value={numero} onChange={(e) => setNumero(e.target.value)} required maxLength={10} />
        </div>
      </div>

      {buscandoCep && <p className="text-muted">Buscando endereço...</p>}
      {enderecoPreview && !buscandoCep && (
        <p className="text-muted">
          {enderecoPreview.logradouro}, {enderecoPreview.bairro} — {enderecoPreview.cidade}/{enderecoPreview.estado}
        </p>
      )}

      <div className="form-group">
        <label>Complemento</label>
        <input value={complemento} onChange={(e) => setComplemento(e.target.value)} maxLength={100} />
      </div>

      {podeEditarConfiguracoes && (
        <>
          <div className="form-group">
            <label>
              Tolerância para agendamentos (minutos)
              <CampoAjuda texto="Tempo que o cliente tem para chegar após o horário reservado antes do agendamento expirar automaticamente." />
            </label>
            <input
              type="number" min={5} max={120}
              value={toleranciaPadraoMinutos}
              onChange={(e) => setToleranciaPadraoMinutos(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label>
              Tempo médio de atendimento (minutos)
              <CampoAjuda texto="Tempo médio que um grupo ocupa uma mesa. Usado para estimar a espera na fila e verificar disponibilidade de mesas nos agendamentos." />
            </label>
            <input
              type="number" min={5} max={240}
              value={tempoMedioAtendimentoMinutos}
              onChange={(e) => setTempoMedioAtendimentoMinutos(e.target.value)}
              required
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Horário de abertura</label>
              <input
                type="time"
                value={horarioAbertura}
                onChange={(e) => setHorarioAbertura(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label>Horário de fechamento</label>
              <input
                type="time"
                value={horarioFechamento}
                onChange={(e) => setHorarioFechamento(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label>
              Antecedência máxima para agendamentos (dias)
              <CampoAjuda texto="Limite de dias no futuro em que um cliente pode agendar um horário." />
            </label>
            <input
              type="number" min={1} max={90}
              value={antecedenciaMaximaDias}
              onChange={(e) => setAntecedenciaMaximaDias(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label>
              Antecedência de reserva de mesa (minutos)
              <CampoAjuda texto="Quanto tempo antes do horário do agendamento uma mesa é reservada automaticamente para o cliente." />
            </label>
            <input
              type="number" min={0} max={120}
              value={antecedenciaReservaMesaMinutos}
              onChange={(e) => setAntecedenciaReservaMesaMinutos(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label>
              Antecedência mínima para agendar (minutos)
              <CampoAjuda texto="Tempo mínimo de antecedência, a partir de agora, para um cliente criar um agendamento." />
            </label>
            <input
              type="number" min={0} max={1440}
              value={antecedenciaMinimaMinutos}
              onChange={(e) => setAntecedenciaMinimaMinutos(e.target.value)}
              required
            />
          </div>
        </>
      )}

      <div className="btn-row">
        <button type="submit" disabled={salvando}>
          {salvando ? 'Salvando...' : 'Salvar'}
        </button>
        {aoCancelar && (
          <button type="button" className="secondary" onClick={aoCancelar} disabled={salvando}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  )
}
