import { useMemo, useState } from 'react'
import { chamar } from '../lib/api'
import { Campo, Mensagem, estilos } from '../components/ui'

// Limites da API: 20 horários por envio, de 10 minutos a 60 dias à frente, até 300 vagos ao mesmo tempo.
const POR_ENVIO = 20
const MAX_VAGOS = 300
const ANTECEDENCIA_MS = 15 * 60 * 1000
const HORIZONTE_MS = 59 * 24 * 3600 * 1000

const DIAS = [
  [1, 'Seg'], [2, 'Ter'], [3, 'Qua'], [4, 'Qui'], [5, 'Sex'], [6, 'Sáb'], [0, 'Dom'],
]

const minutos = (hhmm) => {
  const m = /^(\d{1,2})[:h](\d{2})$/.exec(hhmm.trim())
  if (!m) return null
  const total = Number(m[1]) * 60 + Number(m[2])
  return total >= 0 && total < 24 * 60 ? total : null
}

/** Monta os inícios (ISO) de todos os horários do padrão semanal, sem repetir os que já existem. */
export function gerarHorarios({ dias, inicio, fim, intervalo, duracao, semanas }, existentes, agora = Date.now()) {
  const ini = minutos(inicio)
  const ate = minutos(fim)
  if (ini === null || ate === null || ate <= ini || intervalo < 5 || duracao < 10) return []
  const ja = new Set(existentes)
  const lista = []
  const base = new Date(agora)
  base.setHours(0, 0, 0, 0)
  for (let d = 0; d < semanas * 7; d++) {
    const dia = new Date(base.getFullYear(), base.getMonth(), base.getDate() + d)
    if (!dias.includes(dia.getDay())) continue
    for (let m = ini; m + duracao <= ate; m += intervalo) {
      const h = new Date(dia.getFullYear(), dia.getMonth(), dia.getDate(), Math.floor(m / 60), m % 60)
      const t = h.getTime()
      if (t < agora + ANTECEDENCIA_MS || t > agora + HORIZONTE_MS) continue
      const iso = h.toISOString()
      if (!ja.has(iso)) lista.push(iso)
    }
  }
  return lista
}

/** Publicar de uma vez os horários de uma ou mais semanas, num padrão que se repete. */
export function PublicarSemana({ id, agenda, aoPublicar }) {
  const [dias, setDias] = useState([2, 3, 4, 5, 6])
  const [inicio, setInicio] = useState('09:00')
  const [fim, setFim] = useState('18:00')
  const [intervalo, setIntervalo] = useState('30')
  const [semanas, setSemanas] = useState('1')
  const [servico, setServico] = useState('')
  const [duracao, setDuracao] = useState('30')
  const [valor, setValor] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [progresso, setProgresso] = useState('')
  const [erro, setErro] = useState('')

  const existentes = useMemo(() => (agenda ?? [])
    .filter((h) => h.status !== 'CANCELADO')
    .map((h) => new Date(h.inicio).toISOString()), [agenda])
  const vagosHoje = useMemo(() => (agenda ?? [])
    .filter((h) => h.status === 'LIVRE' && new Date(h.inicio).getTime() > Date.now()).length, [agenda])
  const horarios = useMemo(() => gerarHorarios({
    dias, inicio, fim, intervalo: Number(intervalo), duracao: Number(duracao), semanas: Number(semanas),
  }, existentes), [dias, inicio, fim, intervalo, duracao, semanas, existentes])
  const cabem = MAX_VAGOS - vagosHoje
  const alternar = (d) => setDias(dias.includes(d) ? dias.filter((x) => x !== d) : [...dias, d])

  const publicar = async (e) => {
    e.preventDefault()
    setErro('')
    if (minutos(inicio) === null || minutos(fim) === null) return setErro('Escreva o horário assim: 09:00')
    if (minutos(fim) <= minutos(inicio)) return setErro('O fim do expediente precisa ser depois do início.')
    if (horarios.length === 0) return setErro('Nenhum horário novo nesse padrão. Confira os dias e o expediente.')
    if (horarios.length > cabem) {
      return setErro(`Seriam ${horarios.length} horários, mas cabem só mais ${cabem} vagos ao mesmo tempo (limite de ${MAX_VAGOS}). Escolha menos semanas ou dias.`)
    }
    const centavos = valor.trim() ? Math.round(Number(valor.replace(/\./g, '').replace(',', '.')) * 100) : null
    if (centavos !== null && Number.isNaN(centavos)) return setErro('Preço inválido. Ex.: 45,00')
    setEnviando(true)
    let feitos = 0
    try {
      let agendaNova = null
      for (let i = 0; i < horarios.length; i += POR_ENVIO) {
        const lote = horarios.slice(i, i + POR_ENVIO)
        setProgresso(`Publicando ${Math.min(i + lote.length, horarios.length)} de ${horarios.length}…`)
        agendaNova = await chamar('POST', `/estabelecimentos/${id}/horarios`, {
          servico, duracaoMin: Number(duracao), precoCentavos: centavos, inicios: lote,
        })
        feitos += lote.length
      }
      aoPublicar(agendaNova, `${feitos} horários publicados.`)
    } catch (err) {
      setErro(feitos > 0
        ? `${feitos} horários foram publicados, mas o resto não: ${err.message}`
        : err.message)
      if (feitos > 0) aoPublicar(await chamar('GET', `/estabelecimentos/${id}/agenda`).catch(() => agenda), '')
    } finally {
      setEnviando(false)
      setProgresso('')
    }
  }

  return (
    <form className="grid gap-3 sm:grid-cols-2" noValidate onSubmit={publicar}>
      <fieldset className="sm:col-span-2">
        <legend className="text-sm font-semibold">Dias da semana</legend>
        <div className="mt-1 flex flex-wrap gap-2">
          {DIAS.map(([d, rotulo]) => (
            <label key={d} className={`inline-flex h-10 cursor-pointer items-center gap-2 rounded-lg border px-3 text-sm font-semibold ${
              dias.includes(d) ? 'border-primary bg-primary/10 text-foreground' : 'border-border text-muted'}`}>
              <input type="checkbox" className="h-4 w-4 accent-[#55B02E]" checked={dias.includes(d)} onChange={() => alternar(d)} />
              {rotulo}
            </label>
          ))}
        </div>
      </fieldset>
      <div className="grid grid-cols-2 gap-3">
        <Campo rotulo="Abre às" value={inicio} onChange={(e) => setInicio(e.target.value)} placeholder="09:00" inputMode="numeric" />
        <Campo rotulo="Fecha às" value={fim} onChange={(e) => setFim(e.target.value)} placeholder="18:00" inputMode="numeric" />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <Campo rotulo="Um horário a cada (min)" type="number" min={5} max={480} value={intervalo} onChange={(e) => setIntervalo(e.target.value)} />
        <div>
          <label htmlFor="semanas" className="text-sm font-semibold">Por quantas semanas</label>
          <select id="semanas" value={semanas} onChange={(e) => setSemanas(e.target.value)}
            className="mt-1 h-11 w-full rounded-xl border border-border bg-surface px-3 text-foreground">
            <option value="1">Próximos 7 dias</option>
            <option value="2">Próximas 2 semanas</option>
            <option value="4">Próximas 4 semanas</option>
          </select>
        </div>
      </div>
      <Campo rotulo="Serviço" maxLength={80} value={servico} onChange={(e) => setServico(e.target.value)} placeholder="Ex.: Corte masculino" />
      <div className="grid grid-cols-2 gap-3">
        <Campo rotulo="Duração (min)" type="number" min={10} max={480} value={duracao} onChange={(e) => setDuracao(e.target.value)} />
        <Campo rotulo="Preço (opcional)" inputMode="decimal" value={valor} onChange={(e) => setValor(e.target.value)} placeholder="45,00" />
      </div>
      <p className="text-sm text-muted sm:col-span-2" aria-live="polite">
        {progresso || (horarios.length > 0
          ? `Vão ser publicados ${horarios.length} horários. Os que você já publicou não se repetem.`
          : 'Nenhum horário novo nesse padrão.')}
      </p>
      <div className="sm:col-span-2"><Mensagem tipo="erro">{erro}</Mensagem></div>
      <button type="submit" className={`${estilos.primario} sm:col-span-2`}
        disabled={enviando || horarios.length === 0 || servico.trim().length < 2 || dias.length === 0}>
        {enviando ? 'Publicando…' : `Publicar ${horarios.length} horários`}
      </button>
    </form>
  )
}
