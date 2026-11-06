import { useEffect, useId, useState } from 'react'
import { Link } from 'react-router-dom'
import { Clock, MapPin } from 'lucide-react'
import { TIPOS, UFS, chamar, horaDe, porDia, preco } from '../lib/api'
import { Mensagem, estilos, useEnvio } from '../components/ui'

const PREFERENCIA = 'valtrea-agora:busca'

function lerPreferencia() {
  try {
    return JSON.parse(localStorage.getItem(PREFERENCIA)) || {}
  } catch {
    return {}
  }
}

export function Selecao({ rotulo, valor, aoMudar, opcoes, vazio }) {
  const id = useId()
  return (
    <div>
      <label htmlFor={id} className="text-sm font-semibold">{rotulo}</label>
      <select id={id} value={valor} onChange={(e) => aoMudar(e.target.value)}
        className="mt-1 h-11 w-full rounded-lg border border-border bg-background px-3 text-foreground">
        {vazio !== undefined && <option value="">{vazio}</option>}
        {opcoes.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
      </select>
    </div>
  )
}

export const opcoesUf = UFS.map((uf) => [uf, uf])
export const opcoesTipo = Object.entries(TIPOS)

export function Detalhes({ h }) {
  const valor = preco(h.precoCentavos)
  return (
    <span className="text-sm text-muted">
      {h.servico} · {h.duracaoMin} min{valor ? ` · ${valor}` : ''}
    </span>
  )
}

/** Busca de horários vagos dos próximos 7 dias (aberta, sem conta). Lembra o último filtro neste navegador. */
export function BuscaHorarios() {
  const inicial = lerPreferencia()
  const [uf, setUf] = useState(inicial.uf || '')
  const [busca, setBusca] = useState(inicial.busca || '')
  const [tipo, setTipo] = useState(inicial.tipo || '')
  const [lista, setLista] = useState(null)
  const { enviando, erro, executar } = useEnvio()
  const idBusca = useId()

  const procurar = () => executar(async () => {
    try {
      localStorage.setItem(PREFERENCIA, JSON.stringify({ uf, busca, tipo }))
    } catch {
      // sem armazenamento: só não lembra o filtro
    }
    const q = new URLSearchParams()
    if (uf) q.set('uf', uf)
    if (tipo) q.set('tipo', tipo)
    if (busca.trim()) q.set('busca', busca.trim())
    setLista(await chamar('GET', `/publico/horarios?${q}`, null, { autenticar: false }))
  })

  useEffect(() => {
    procurar()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div>
      <form onSubmit={(e) => { e.preventDefault(); procurar() }} role="search" aria-label="Procurar horários"
        className="grid gap-3 sm:grid-cols-[7rem_1fr_12rem_auto] sm:items-end">
        <Selecao rotulo="Estado" valor={uf} aoMudar={setUf} opcoes={opcoesUf} vazio="Todos" />
        <div>
          <label htmlFor={idBusca} className="text-sm font-semibold">Cidade ou bairro</label>
          <input id={idBusca} value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Ex.: Tijuca"
            className="mt-1 h-11 w-full rounded-lg border border-border bg-background px-3 text-foreground placeholder:text-muted" />
        </div>
        <Selecao rotulo="Serviço" valor={tipo} aoMudar={setTipo} opcoes={opcoesTipo} vazio="Todos" />
        <button type="submit" className={estilos.primario} disabled={enviando}>{enviando ? 'Procurando…' : 'Procurar'}</button>
      </form>

      <div className="mt-6" aria-live="polite">
        <Mensagem tipo="erro">{erro}</Mensagem>
        {lista?.length === 0 && (
          <p className="rounded-xl border border-dashed border-border p-6 text-center text-muted">
            Nenhum horário vago nos próximos 7 dias com esses filtros. Tente outra cidade ou outro serviço.
          </p>
        )}
        {lista && porDia(lista).map(([dia, itens]) => (
          <section key={dia} aria-label={dia} className="mb-6">
            <h2 className="mb-3 text-lg font-bold capitalize">{dia}</h2>
            <ul className="grid gap-3 sm:grid-cols-2">
              {itens.map((h) => (
                <li key={h.id}>
                  <Link to={`/e/${h.estabelecimento.id}#h-${h.id}`}
                    className={`${estilos.cartao} flex items-start gap-4 transition-colors hover:border-primary/60`}>
                    <span className="flex min-w-16 flex-col items-center rounded-lg bg-surface-2 px-2 py-1.5">
                      <Clock className="h-4 w-4 text-primary" aria-hidden="true" />
                      <span className="text-lg font-bold tabular-nums">{horaDe(h.inicio)}</span>
                    </span>
                    <span className="flex flex-col">
                      <span className="font-bold">{h.estabelecimento.nome}</span>
                      <Detalhes h={h} />
                      <span className="mt-1 flex items-center gap-1 text-xs text-muted">
                        <MapPin className="h-3 w-3" aria-hidden="true" />
                        {h.estabelecimento.bairro}, {h.estabelecimento.cidade} - {h.estabelecimento.uf}
                      </span>
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </div>
  )
}
