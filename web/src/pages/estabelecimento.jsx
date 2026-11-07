import { useEffect, useId, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Flag, MapPin, Phone } from 'lucide-react'
import { MOTIVOS, TIPOS, chamar, horaDe, porDia, quando } from '../lib/api'
import { useAuth } from '../lib/auth'
import { Campo, Mensagem, Pagina, estilos, useEnvio } from '../components/ui'
import { Detalhes } from './horarios'

function Denunciar({ id, aoDenunciar }) {
  const [aberto, setAberto] = useState(false)
  const [motivo, setMotivo] = useState('')
  const { enviando, erro, executar } = useEnvio()
  const nome = useId()
  if (!aberto) {
    return (
      <button type="button" className="inline-flex h-10 items-center gap-1.5 text-sm font-semibold text-muted hover:text-danger" onClick={() => setAberto(true)}>
        <Flag className="h-4 w-4" aria-hidden="true" /> Denunciar este estabelecimento
      </button>
    )
  }
  return (
    <form className={`${estilos.cartao} space-y-3`} onSubmit={(e) => {
      e.preventDefault()
      executar(async () => {
        await chamar('POST', `/estabelecimentos/${id}/denuncia`, { motivo })
        aoDenunciar()
      })
    }}>
      <fieldset>
        <legend className="font-semibold">Qual o problema?</legend>
        <p className="mb-2 text-sm text-muted">Com 3 denúncias, o estabelecimento sai da busca até a nossa revisão. Ninguém fica sabendo que foi você.</p>
        {Object.entries(MOTIVOS).map(([valor, rotulo]) => (
          <label key={valor} className="flex min-h-10 items-center gap-2.5 text-sm">
            <input type="radio" name={nome} value={valor} checked={motivo === valor} onChange={() => setMotivo(valor)} className="h-4 w-4 accent-[#55B02E]" />
            {rotulo}
          </label>
        ))}
      </fieldset>
      <Mensagem tipo="erro">{erro}</Mensagem>
      <div className="flex flex-col gap-2 sm:flex-row">
        <button type="submit" className={estilos.perigo} disabled={enviando || !motivo}>Enviar denúncia</button>
        <button type="button" className={estilos.secundario} onClick={() => setAberto(false)}>Cancelar</button>
      </div>
    </form>
  )
}

function Reservar({ horario, aoCancelar }) {
  const navegar = useNavigate()
  const [telefone, setTelefone] = useState('')
  const [observacao, setObservacao] = useState('')
  const { enviando, erro, executar } = useEnvio()
  return (
    <form className="mt-3 space-y-3 rounded-xl border border-primary/50 bg-surface-2 p-4" onSubmit={(e) => {
      e.preventDefault()
      executar(async () => {
        await chamar('POST', `/horarios/${horario.id}/reserva`, { telefone: telefone || null, observacao: observacao || null })
        navegar('/reservas', { state: { aviso: 'Reserva feita! O estabelecimento já foi avisado. Toque em "Adicionar à agenda" para o celular lembrar você.' } })
      })
    }}>
      <p className="font-semibold">Reservar {horario.servico}, {quando(horario.inicio)}</p>
      <Campo rotulo="Seu telefone (opcional)" type="tel" autoComplete="tel" maxLength={20} value={telefone}
        onChange={(e) => setTelefone(e.target.value)} dica="Só o estabelecimento vê, para falar com você sobre esta reserva." />
      <Campo rotulo="Observação (opcional)" maxLength={200} value={observacao} onChange={(e) => setObservacao(e.target.value)} />
      <p className="text-xs text-muted">O pagamento é no local. Nunca pague adiantado nem por Pix para garantir o horário.</p>
      <Mensagem tipo="erro">{erro}</Mensagem>
      <div className="flex flex-col gap-2 sm:flex-row">
        <button type="submit" className={estilos.primario} disabled={enviando}>{enviando ? 'Reservando…' : 'Confirmar reserva'}</button>
        <button type="button" className={estilos.secundario} onClick={aoCancelar}>Voltar</button>
      </div>
    </form>
  )
}

export function EstabelecimentoPage() {
  const { id } = useParams()
  const { perfil } = useAuth()
  const navegar = useNavigate()
  const [dados, setDados] = useState(null)
  const [naoExiste, setNaoExiste] = useState(false)
  const [escolhido, setEscolhido] = useState(null)

  useEffect(() => {
    const pedido = perfil ? chamar('GET', `/estabelecimentos/${id}`) : chamar('GET', `/publico/estabelecimentos/${id}`, null, { autenticar: false })
    pedido.then(setDados).catch((e) => (e.status === 404 ? setNaoExiste(true) : null))
  }, [id, perfil?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  if (naoExiste) {
    return (
      <Pagina titulo="Estabelecimento não encontrado" largura="max-w-3xl">
        <p className="text-muted">Ele pode ter saído do Agora ou estar em análise.</p>
        <Link to="/" className={`${estilos.secundario} mt-4`}>Ver outros horários</Link>
      </Pagina>
    )
  }
  if (!dados) return <Pagina largura="max-w-3xl"><p className="text-muted">Carregando…</p></Pagina>
  const e = dados.estabelecimento

  return (
    <Pagina largura="max-w-3xl">
      <p className="text-sm font-semibold text-primary">{TIPOS[e.tipo]}</p>
      <h1 className="mt-1 text-3xl font-bold tracking-tight">{e.nome}</h1>
      {e.descricao && <p className="mt-3 whitespace-pre-line text-muted">{e.descricao}</p>}
      <p className="mt-4 flex items-start gap-2 text-sm"><MapPin className="mt-0.5 h-4 w-4 shrink-0 text-primary" aria-hidden="true" />
        {e.endereco} · {e.bairro}, {e.cidade} - {e.uf}</p>
      <p className="mt-2 flex items-center gap-2 text-sm"><Phone className="h-4 w-4 text-primary" aria-hidden="true" />
        <a href={`tel:${e.telefone.replace(/[^0-9+]/g, '')}`} className={estilos.link}>{e.telefone}</a></p>

      <section aria-labelledby="horarios" className="mt-8">
        <h2 id="horarios" className="text-xl font-bold">Horários vagos</h2>
        {dados.horarios.length === 0 && <p className="mt-2 text-muted">Nenhum horário vago nos próximos 14 dias.</p>}
        {porDia(dados.horarios).map(([dia, itens]) => (
          <div key={dia} className="mt-5">
            <h3 className="mb-2 font-semibold capitalize">{dia}</h3>
            <ul className="space-y-2">
              {itens.map((h) => (
                <li key={h.id} id={`h-${h.id}`} className="rounded-xl border border-border p-3">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-lg font-bold tabular-nums">{horaDe(h.inicio)}</p>
                      <Detalhes h={h} />
                    </div>
                    {!dados.meu && escolhido?.id !== h.id && (
                      <button type="button" className={estilos.primario}
                        onClick={() => (perfil ? setEscolhido(h) : navegar('/cadastro'))}
                        aria-label={`Reservar ${h.servico} às ${horaDe(h.inicio)}`}>
                        Reservar
                      </button>
                    )}
                  </div>
                  {escolhido?.id === h.id && <Reservar horario={h} aoCancelar={() => setEscolhido(null)} />}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </section>

      {dados.meu && <Mensagem>Este estabelecimento é seu. Gerencie a agenda em <Link to={`/negocio/${e.id}`} className={estilos.link}>Meu negócio</Link>.</Mensagem>}
      {perfil?.emailConfirmado && !dados.meu && (
        <div className="mt-10">
          {dados.denunciei ? (
            <p className="text-sm text-muted">Você denunciou este estabelecimento. Obrigado por avisar.</p>
          ) : (
            <Denunciar id={e.id} aoDenunciar={() => setDados({ ...dados, denunciei: true })} />
          )}
        </div>
      )}
    </Pagina>
  )
}
