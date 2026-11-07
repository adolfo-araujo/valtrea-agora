import { Link } from 'react-router-dom'
import { CalendarCheck, Search, ShieldCheck, Store } from 'lucide-react'
import { useAuth } from '../lib/auth'
import { Pagina, estilos, ComoInstalarNoIphone } from '../components/ui'
import { BuscaHorarios } from './horarios'

const PASSOS = [
  { icone: Search, titulo: 'Procure perto de você', texto: 'Horários que acabaram de vagar em salões, barbearias e outros serviços da sua cidade.' },
  { icone: CalendarCheck, titulo: 'Reserve com um toque', texto: 'Sem pagar nada no app: o horário fica seu e você paga direto no local.' },
  { icone: Store, titulo: 'Tem um negócio?', texto: 'Cadastre o seu estabelecimento e publique os horários vagos do dia para não ficar com a cadeira vazia.', link: ['/para-negocios', 'Saiba como funciona para negócios'] },
]

export function InicioPage() {
  const { perfil } = useAuth()
  return (
    <Pagina>
      {!perfil && (
        <section className="py-8 sm:py-12">
          <p className="text-sm font-bold text-primary">Valtrea Agora · em testes</p>
          <h1 className="mt-2 max-w-2xl text-4xl font-black leading-tight tracking-tight sm:text-5xl">
            Oportunidades que acontecem perto de você.
          </h1>
          <p className="mt-5 max-w-xl text-lg leading-relaxed text-muted">
            Horários vagos de salões, barbearias e outros serviços da sua região, para reservar ainda hoje.
          </p>
          <div className="mt-8 flex flex-col gap-3 sm:flex-row">
            <Link to="/cadastro" className={estilos.primario}>Criar conta grátis</Link>
            <Link to="/entrar" className={estilos.secundario}>Já tenho conta</Link>
          </div>
          <ComoInstalarNoIphone />
      </section>
      )}
      <section aria-labelledby="horarios-vagos" className={perfil ? '' : estilos.cartao}>
        {perfil ? (
          <h1 id="horarios-vagos" className="mb-6 text-2xl font-bold tracking-tight sm:text-3xl">Horários vagos</h1>
        ) : (
          <h2 id="horarios-vagos" className="mb-4 text-xl font-bold">Horários vagos agora</h2>
        )}
        <BuscaHorarios />
      </section>
      {!perfil && (
        <section aria-labelledby="como-funciona" className="mt-10 border-t border-border py-10">
          <h2 id="como-funciona" className="text-2xl font-bold">Como funciona</h2>
          <ol className="mt-8 grid gap-6 sm:grid-cols-3">
            {PASSOS.map(({ icone: Icone, titulo, texto, link }) => (
              <li key={titulo} className={estilos.cartao}>
                <Icone className="h-6 w-6 text-primary" aria-hidden="true" />
                <h3 className="mt-3 font-bold">{titulo}</h3>
                <p className="mt-1 text-sm leading-relaxed text-muted">{texto}</p>
                {link && <Link to={link[0]} className={`${estilos.link} mt-2 inline-block text-sm`}>{link[1]} →</Link>}
              </li>
            ))}
          </ol>
          <p className="mt-8 flex items-start gap-2 text-sm text-muted">
            <ShieldCheck className="h-5 w-5 shrink-0 text-primary" aria-hidden="true" />
            Todo estabelecimento passa por análise antes de aparecer. Nunca pague adiantado nem por Pix para garantir um horário.
          </p>
        </section>
      )}
    </Pagina>
  )
}
