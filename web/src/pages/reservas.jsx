import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { CalendarPlus, MapPin, Phone } from 'lucide-react'
import { STATUS_RESERVA, chamar, preco, quando } from '../lib/api'
import { Mensagem, Pagina, Vazio, estilos, useEnvio } from '../components/ui'
import { baixarIcs, linkGoogleAgenda } from '../lib/calendario'

export function ReservasPage() {
  const avisoAnterior = useLocation().state?.aviso
  const [lista, setLista] = useState(null)
  const [aviso, setAviso] = useState(avisoAnterior || '')
  const { enviando, erro, executar } = useEnvio()
  const [semInternet, setSemInternet] = useState(!navigator.onLine)
  useEffect(() => {
    const atualizar = () => setSemInternet(!navigator.onLine)
    window.addEventListener('online', atualizar)
    window.addEventListener('offline', atualizar)
    return () => {
      window.removeEventListener('online', atualizar)
      window.removeEventListener('offline', atualizar)
    }
  }, [])
  const carregar = () => executar(async () => setLista(await chamar('GET', '/eu/reservas')))
  useEffect(() => {
    carregar()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const agora = Date.now()
  const proximas = (lista || []).filter((r) => r.status === 'CONFIRMADA' && new Date(r.inicio).getTime() > agora).reverse()
  const historico = (lista || []).filter((r) => !proximas.includes(r))

  const Cartao = ({ r, podeCancelar }) => (
    <li className={estilos.cartao}>
      <p className="text-sm font-semibold text-primary">{STATUS_RESERVA[r.status]}</p>
      <h3 className="mt-1 text-lg font-bold capitalize">{quando(r.inicio)}</h3>
      <p className="text-sm">{r.servico} · {r.duracaoMin} min{r.precoCentavos != null ? ` · ${preco(r.precoCentavos)}` : ''}</p>
      <p className="mt-2 font-semibold"><Link to={`/e/${r.estabelecimento.id}`} className={estilos.link}>{r.estabelecimento.nome}</Link></p>
      <p className="mt-1 flex items-start gap-1.5 text-sm text-muted"><MapPin className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
        {r.estabelecimento.endereco} · {r.estabelecimento.bairro}, {r.estabelecimento.cidade} - {r.estabelecimento.uf}</p>
      <p className="mt-1 flex items-center gap-1.5 text-sm text-muted"><Phone className="h-4 w-4" aria-hidden="true" />
        <a href={`tel:${r.estabelecimento.telefone.replace(/[^0-9+]/g, '')}`} className={estilos.link}>{r.estabelecimento.telefone}</a></p>
      {podeCancelar && (
        <div className="mt-4 flex flex-wrap gap-2">
          <button type="button" className={estilos.secundario} onClick={() => baixarIcs(r)}>
            <CalendarPlus className="h-4 w-4" aria-hidden="true" /> Adicionar à agenda
          </button>
          <a className={estilos.secundario} href={linkGoogleAgenda(r)} target="_blank" rel="noreferrer">Google Agenda</a>
        </div>
      )}
      {podeCancelar && (
        <p className="mt-2 text-xs text-muted">A agenda do celular avisa 1 dia e 2 horas antes.</p>
      )}
      {podeCancelar && (
        <button type="button" className={`${estilos.perigo} mt-4`} disabled={enviando} onClick={() => {
          if (!window.confirm('Cancelar esta reserva? O horário volta a ficar livre para outra pessoa.')) return
          executar(async () => {
            await chamar('POST', `/reservas/${r.id}/cancelar`)
            setAviso('Reserva cancelada. Avisamos o estabelecimento.')
            setLista(await chamar('GET', '/eu/reservas'))
          })
        }}>Cancelar reserva</button>
      )}
    </li>
  )

  return (
    <Pagina titulo="Minhas reservas">
      <Mensagem>{aviso}</Mensagem>
      <Mensagem>{semInternet && lista ? 'Sem internet: esta é a última versão salva das suas reservas. Para cancelar ou ver mudanças, conecte-se de novo.' : ''}</Mensagem>
      <Mensagem tipo="erro">{erro}</Mensagem>
      {lista?.length === 0 && (
        <Vazio titulo="Nenhuma reserva ainda" texto="Procure pela sua cidade ou bairro, escolha um horário vago e reserve em segundos. As reservas aparecem aqui." acao={['Ver horários vagos', '/']} />
      )}
      {proximas.length > 0 && (
        <section aria-labelledby="proximas" className="mt-4">
          <h2 id="proximas" className="mb-3 text-xl font-bold">Próximas</h2>
          <ul className="grid gap-4 sm:grid-cols-2">{proximas.map((r) => <Cartao key={r.id} r={r} podeCancelar />)}</ul>
        </section>
      )}
      {historico.length > 0 && (
        <section aria-labelledby="anteriores" className="mt-8">
          <h2 id="anteriores" className="mb-3 text-xl font-bold">Anteriores e canceladas</h2>
          <ul className="grid gap-4 sm:grid-cols-2">{historico.map((r) => <Cartao key={r.id} r={r} />)}</ul>
        </section>
      )}
      <p className="mt-8 text-xs text-muted">Até 3 reservas marcadas ao mesmo tempo. As reservas são apagadas 90 dias depois do horário.</p>
    </Pagina>
  )
}
