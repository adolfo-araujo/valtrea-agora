import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { STATUS_ESTABELECIMENTO, STATUS_RESERVA, TIPOS, UFS, chamar, horaDe, porDia } from '../lib/api'
import { useAuth } from '../lib/auth'
import { Campo, Mensagem, Pagina, estilos, useEnvio } from '../components/ui'
import { Detalhes, Selecao, opcoesTipo } from './horarios'
import { Divulgar } from './divulgar'
import { PublicarSemana } from './semana'

const VAZIO = { nome: '', tipo: 'SALAO', descricao: '', endereco: '', bairro: '', cidade: '', uf: 'RJ', telefone: '' }

function FormEstabelecimento({ inicial, aoSalvar, rotulo }) {
  const [d, setD] = useState({ ...VAZIO, ...inicial, descricao: inicial?.descricao ?? '' })
  const { enviando, erro, executar } = useEnvio()
  const muda = (campo) => (e) => setD({ ...d, [campo]: e.target.value })
  const pronto = d.nome.trim().length >= 2 && d.endereco.trim().length >= 5 && d.bairro.trim().length >= 2 &&
    d.cidade.trim().length >= 2 && d.telefone.replace(/\D/g, '').length >= 8
  return (
    <form className="grid gap-3 sm:grid-cols-2" noValidate onSubmit={(e) => {
      e.preventDefault()
      executar(() => aoSalvar({ ...d, descricao: d.descricao.trim() || null }))
    }}>
      <Campo rotulo="Nome do estabelecimento" maxLength={80} value={d.nome} onChange={muda('nome')} />
      <Selecao rotulo="Tipo" valor={d.tipo} aoMudar={(tipo) => setD({ ...d, tipo })} opcoes={opcoesTipo} />
      <Campo rotulo="Endereço (rua e número)" maxLength={160} value={d.endereco} onChange={muda('endereco')} className="sm:col-span-2" />
      <Campo rotulo="Bairro" maxLength={80} value={d.bairro} onChange={muda('bairro')} />
      <Campo rotulo="Cidade" maxLength={80} value={d.cidade} onChange={muda('cidade')} />
      <Selecao rotulo="Estado" valor={d.uf} aoMudar={(uf) => setD({ ...d, uf })} opcoes={UFS.map((u) => [u, u])} />
      <Campo rotulo="Telefone do estabelecimento" type="tel" maxLength={20} value={d.telefone} onChange={muda('telefone')}
        dica="Com DDD. Aparece para os clientes." />
      <Campo rotulo="Descrição (opcional)" as="textarea" maxLength={500} value={d.descricao} onChange={muda('descricao')} className="sm:col-span-2"
        dica="Serviços, diferenciais. Sem pedir pagamento antecipado." />
      <div className="sm:col-span-2"><Mensagem tipo="erro">{erro}</Mensagem></div>
      <button type="submit" className={`${estilos.primario} sm:col-span-2`} disabled={enviando || !pronto}>{rotulo}</button>
    </form>
  )
}

function Situacao({ e }) {
  const cor = e.status === 'APROVADO' ? 'text-primary' : e.status === 'RECUSADO' ? 'text-danger' : 'text-muted'
  return (
    <div>
      <p className={`text-sm font-semibold ${cor}`}>{STATUS_ESTABELECIMENTO[e.status]}</p>
      {e.status === 'EM_ANALISE' && <p className="text-xs text-muted">Avisamos por e-mail quando terminarmos. Você já pode publicar horários.</p>}
      {e.status === 'RECUSADO' && <p className="text-sm">Motivo: {e.motivoRecusa}. Corrija e salve para voltar à análise.</p>}
    </div>
  )
}

export function NegocioPage() {
  const { perfil } = useAuth()
  const navegar = useNavigate()
  const [lista, setLista] = useState(null)
  const [criando, setCriando] = useState(false)
  const { erro, executar } = useEnvio()
  useEffect(() => {
    executar(async () => setLista(await chamar('GET', '/eu/estabelecimentos')))
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <Pagina titulo="Meu negócio" largura="max-w-3xl">
      <p className="mb-6 text-muted">Cadastre o seu salão, barbearia ou outro serviço e publique os horários que ficaram vagos.
        Todo cadastro passa por análise antes de aparecer na busca.</p>
      {!perfil?.emailConfirmado && <Mensagem>Confirme seu e-mail antes. <Link to="/confirmar-email" className={estilos.link}>Confirmar agora</Link></Mensagem>}
      <Mensagem tipo="erro">{erro}</Mensagem>
      <ul className="space-y-4">
        {lista?.map((e) => (
          <li key={e.id}>
            <Link to={`/negocio/${e.id}`} className={`${estilos.cartao} block hover:border-primary/60`}>
              <p className="text-xs text-muted">{TIPOS[e.tipo]} · {e.bairro}, {e.cidade} - {e.uf}</p>
              <h2 className="text-lg font-bold">{e.nome}</h2>
              <Situacao e={e} />
            </Link>
          </li>
        ))}
      </ul>
      {criando ? (
        <section aria-labelledby="novo" className={`${estilos.cartao} mt-6`}>
          <h2 id="novo" className="mb-4 text-lg font-bold">Novo estabelecimento</h2>
          <FormEstabelecimento rotulo="Enviar para análise" aoSalvar={async (d) => {
            const novo = await chamar('POST', '/estabelecimentos', d)
            navegar(`/negocio/${novo.id}`)
          }} />
        </section>
      ) : (
        lista && lista.length < 3 && perfil?.emailConfirmado && (
          <button type="button" className={`${estilos.primario} mt-6`} onClick={() => setCriando(true)}>
            <Plus className="h-4 w-4" aria-hidden="true" /> Cadastrar estabelecimento
          </button>
        )
      )}
    </Pagina>
  )
}

const hoje = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function PublicarHorarios({ id, aoPublicar }) {
  const [data, setData] = useState(hoje())
  const [horas, setHoras] = useState('')
  const [servico, setServico] = useState('')
  const [duracao, setDuracao] = useState('30')
  const [valor, setValor] = useState('')
  const { enviando, erro, setErro, executar } = useEnvio()
  const lista = horas.split(/[,;\s]+/).map((h) => h.trim()).filter(Boolean)
  return (
    <form className="grid gap-3 sm:grid-cols-2" noValidate onSubmit={(e) => {
      e.preventDefault()
      if (lista.some((h) => !/^\d{1,2}[:h]\d{2}$/.test(h))) return setErro('Escreva os horários assim: 14:00, 15:30')
      const inicios = lista.map((h) => {
        const [hh, mm] = h.split(/[:h]/)
        return new Date(`${data}T${hh.padStart(2, '0')}:${mm}:00`).toISOString()
      })
      const centavos = valor.trim() ? Math.round(Number(valor.replace(/\./g, '').replace(',', '.')) * 100) : null
      if (centavos !== null && Number.isNaN(centavos)) return setErro('Preço inválido. Ex.: 45,00')
      executar(async () => {
        aoPublicar(await chamar('POST', `/estabelecimentos/${id}/horarios`, { servico, duracaoMin: Number(duracao), precoCentavos: centavos, inicios }))
        setHoras('')
      })
    }}>
      <Campo rotulo="Dia" type="date" min={hoje()} value={data} onChange={(e) => setData(e.target.value)} />
      <Campo rotulo="Horários vagos" value={horas} onChange={(e) => setHoras(e.target.value)} placeholder="14:00, 15:30"
        dica="Separe por vírgula. Até 20 de cada vez." />
      <Campo rotulo="Serviço" maxLength={80} value={servico} onChange={(e) => setServico(e.target.value)} placeholder="Ex.: Corte masculino" />
      <div className="grid grid-cols-2 gap-3">
        <Campo rotulo="Duração (min)" type="number" min={10} max={480} value={duracao} onChange={(e) => setDuracao(e.target.value)} />
        <Campo rotulo="Preço (opcional)" inputMode="decimal" value={valor} onChange={(e) => setValor(e.target.value)} placeholder="45,00" />
      </div>
      <div className="sm:col-span-2"><Mensagem tipo="erro">{erro}</Mensagem></div>
      <button type="submit" className={`${estilos.primario} sm:col-span-2`}
        disabled={enviando || lista.length === 0 || servico.trim().length < 2}>
        Publicar {lista.length > 1 ? `${lista.length} horários` : 'horário'}
      </button>
    </form>
  )
}

export function AgendaPage() {
  const { id } = useParams()
  const navegar = useNavigate()
  const [est, setEst] = useState(null)
  const [agenda, setAgenda] = useState(null)
  const [editando, setEditando] = useState(false)
  const [aviso, setAviso] = useState('')
  const [modo, setModo] = useState('semana')
  const { enviando, erro, executar } = useEnvio()

  const carregar = () => executar(async () => {
    const meus = await chamar('GET', '/eu/estabelecimentos')
    setEst(meus.find((e) => e.id === id) ?? null)
    setAgenda(await chamar('GET', `/estabelecimentos/${id}/agenda`))
  })
  useEffect(() => {
    carregar()
  }, [id]) // eslint-disable-line react-hooks/exhaustive-deps

  const acao = (fn, mensagem) => executar(async () => {
    await fn()
    setAviso(mensagem)
    setAgenda(await chamar('GET', `/estabelecimentos/${id}/agenda`))
  })

  if (!est) return <Pagina largura="max-w-3xl"><Mensagem tipo="erro">{erro}</Mensagem><p className="text-muted">{erro ? '' : 'Carregando…'}</p></Pagina>
  const agora = Date.now()

  return (
    <Pagina largura="max-w-3xl">
      <Link to="/negocio" className="text-sm font-semibold text-primary hover:underline">← Meu negócio</Link>
      <h1 className="mt-2 text-3xl font-bold tracking-tight">{est.nome}</h1>
      <div className="mt-2"><Situacao e={est} /></div>
      {est.status === 'APROVADO' && <p className="mt-2 text-sm"><Link to={`/e/${est.id}`} className={estilos.link}>Ver como os clientes veem</Link></p>}
      <Mensagem>{aviso}</Mensagem>
      <Mensagem tipo="erro">{erro}</Mensagem>

      {est.status === 'APROVADO' && (
        <section aria-labelledby="divulgar" className={`${estilos.cartao} mt-6`}>
          <h2 id="divulgar" className="mb-3 text-lg font-bold">Divulgue sua página</h2>
          <Divulgar est={est} />
        </section>
      )}

      <section aria-labelledby="publicar" className={`${estilos.cartao} mt-6`}>
        <h2 id="publicar" className="text-lg font-bold">Publicar horários vagos</h2>
        <div role="group" aria-label="Como publicar" className="mb-4 mt-3 inline-flex rounded-lg border border-border bg-surface-2 p-1">
          {[['semana', 'Toda semana'], ['dia', 'Um dia']].map(([valor, rotulo]) => (
            <button key={valor} type="button" aria-pressed={modo === valor} onClick={() => setModo(valor)}
              className={`h-9 rounded-md px-4 text-sm font-semibold ${modo === valor ? 'bg-background text-foreground' : 'text-muted hover:text-foreground'}`}>
              {rotulo}
            </button>
          ))}
        </div>
        {modo === 'semana' ? (
          <PublicarSemana id={id} agenda={agenda} aoPublicar={(a, msg) => { if (a) setAgenda(a); if (msg) setAviso(msg) }} />
        ) : (
          <PublicarHorarios id={id} aoPublicar={(a) => { setAgenda(a); setAviso('Horários publicados.') }} />
        )}
      </section>

      <section aria-labelledby="agenda" className="mt-8">
        <h2 id="agenda" className="text-xl font-bold">Agenda</h2>
        {agenda?.length === 0 && <p className="mt-2 text-muted">Nenhum horário publicado.</p>}
        {agenda && porDia(agenda).map(([dia, itens]) => (
          <div key={dia} className="mt-5">
            <h3 className="mb-2 font-semibold capitalize">{dia}</h3>
            <ul className="space-y-2">
              {itens.map((h) => {
                const passou = new Date(h.inicio).getTime() <= agora
                const r = h.reserva
                return (
                  <li key={h.id} className="rounded-xl border border-border p-3">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div>
                        <p className="font-bold tabular-nums">{horaDe(h.inicio)} <span className="text-sm font-normal text-muted">
                          {h.status === 'LIVRE' ? 'livre' : h.status === 'CANCELADO' ? 'cancelado' : 'reservado'}</span></p>
                        <Detalhes h={h} />
                      </div>
                      {h.status === 'LIVRE' && (
                        <button type="button" className="text-sm font-semibold text-danger hover:underline" disabled={enviando}
                          onClick={() => acao(() => chamar('DELETE', `/horarios/${h.id}`), 'Horário apagado.')}>Apagar</button>
                      )}
                    </div>
                    {r && (
                      <div className="mt-2 rounded-lg bg-surface-2 p-3 text-sm">
                        <p><strong>{r.cliente}</strong> · {STATUS_RESERVA[r.status].replace('por você', 'pelo cliente')}</p>
                        {r.telefone && <p>Telefone: <a href={`tel:${r.telefone.replace(/[^0-9+]/g, '')}`} className={estilos.link}>{r.telefone}</a></p>}
                        {r.observacao && <p>Observação: {r.observacao}</p>}
                        {r.status === 'CONFIRMADA' && !passou && (
                          <button type="button" className={`${estilos.perigo} mt-2`} disabled={enviando} onClick={() => {
                            if (!window.confirm('Cancelar esta reserva? O cliente recebe um e-mail.')) return
                            acao(() => chamar('POST', `/reservas/${r.id}/cancelar`), 'Reserva cancelada. Avisamos o cliente.')
                          }}>Cancelar reserva</button>
                        )}
                        {passou && ['CONFIRMADA', 'ATENDIDA', 'NAO_COMPARECEU'].includes(r.status) && (
                          <div className="mt-2 flex flex-wrap gap-2">
                            <button type="button" className={estilos.secundario} disabled={enviando || r.status === 'ATENDIDA'}
                              onClick={() => acao(() => chamar('POST', `/reservas/${r.id}/status`, { status: 'ATENDIDA' }), 'Marcado como atendido.')}>Atendido</button>
                            <button type="button" className={estilos.secundario} disabled={enviando || r.status === 'NAO_COMPARECEU'}
                              onClick={() => acao(() => chamar('POST', `/reservas/${r.id}/status`, { status: 'NAO_COMPARECEU' }), 'Marcado como não compareceu.')}>Não veio</button>
                          </div>
                        )}
                      </div>
                    )}
                  </li>
                )
              })}
            </ul>
          </div>
        ))}
      </section>

      <section aria-labelledby="cadastro" className={`${estilos.cartao} mt-10`}>
        <h2 id="cadastro" className="text-lg font-bold">Dados do estabelecimento</h2>
        {editando ? (
          <div className="mt-4">
            <FormEstabelecimento inicial={est} rotulo="Salvar" aoSalvar={async (d) => {
              setEst(await chamar('PUT', `/estabelecimentos/${id}`, d))
              setEditando(false)
              setAviso('Dados salvos.')
            }} />
          </div>
        ) : (
          <>
            <p className="mt-1 text-sm text-muted">{est.endereco} · {est.bairro}, {est.cidade} - {est.uf} · {est.telefone}</p>
            <div className="mt-3 flex flex-col gap-2 sm:flex-row">
              <button type="button" className={estilos.secundario} onClick={() => setEditando(true)}>Editar</button>
              <button type="button" className={estilos.perigo} disabled={enviando} onClick={() => {
                if (!window.confirm('Apagar o estabelecimento, os horários e as reservas? Não dá para desfazer.')) return
                executar(async () => {
                  await chamar('DELETE', `/estabelecimentos/${id}`)
                  navegar('/negocio', { replace: true })
                })
              }}>Apagar estabelecimento</button>
            </div>
          </>
        )}
      </section>
    </Pagina>
  )
}
