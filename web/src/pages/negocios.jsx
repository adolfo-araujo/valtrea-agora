import { Link } from 'react-router-dom'
import { CalendarClock, Megaphone, QrCode, ShieldCheck, Smartphone, UserCheck } from 'lucide-react'
import { useAuth } from '../lib/auth'
import { Pagina, estilos } from '../components/ui'

const PASSOS = [
  { titulo: 'Cadastre o seu negócio', texto: 'Nome, endereço, telefone e o tipo de serviço. Analisamos antes de aparecer na busca, para que só negócios reais estejam lá.' },
  { titulo: 'Publique os horários vagos', texto: 'Uma vez por semana: marque os dias, o horário de abrir e de fechar e de quanto em quanto tempo atende. Ou só o horário que vagou hoje.' },
  { titulo: 'Receba as reservas', texto: 'Cada reserva chega por e-mail e na sua agenda do Agora, com o primeiro nome do cliente e, se ele quiser, o telefone.' },
]

const VANTAGENS = [
  { icone: CalendarClock, titulo: 'Menos cadeira vazia', texto: 'O horário que vagou de última hora aparece para quem está procurando perto agora.' },
  { icone: Smartphone, titulo: 'Menos tempo no WhatsApp', texto: 'O cliente vê o que está livre e reserva sozinho, sem perguntar "tem horário hoje?".' },
  { icone: QrCode, titulo: 'Sua página com QR Code', texto: 'Link próprio para a bio do Instagram e QR Code para o balcão, prontos para imprimir.' },
  { icone: UserCheck, titulo: 'Controle de presença', texto: 'Marque quem veio e quem faltou. O cliente pode pôr a reserva na agenda do celular, com lembrete.' },
]

const PERGUNTAS = [
  ['Quanto custa?', 'Nada durante a fase de testes: grátis para clientes e para estabelecimentos. Se um dia houver plano pago, avisamos com antecedência e nada é cobrado sem você aceitar.'],
  ['O cliente paga pelo app?', 'Não. O Agora não recebe pagamentos: o cliente paga direto a você, no atendimento, do jeito que você já aceita.'],
  ['Preciso deixar a agenda inteira no Agora?', 'Não. Você publica só os horários que quer oferecer, por exemplo os que vagaram ou os de dias mais fracos. O resto continua como hoje.'],
  ['E se o cliente faltar?', 'Você marca "Não veio" na agenda. O cliente tem no máximo 3 reservas marcadas ao mesmo tempo e pode cancelar pelo app, e o horário cancelado não volta a ficar disponível sozinho.'],
  ['Posso cancelar uma reserva?', 'Pode. O cliente recebe um e-mail na hora avisando do cancelamento.'],
  ['Quais dados do cliente eu vejo?', 'O primeiro nome e, se o cliente quiser informar, o telefone e uma observação. Use esses dados só para tratar da reserva.'],
  ['Como funciona a análise?', 'Analisamos cada cadastro antes de liberar e você recebe um e-mail quando terminar. Enquanto isso, já pode publicar horários: eles aparecem assim que o cadastro for aprovado.'],
]

export function NegociosPage() {
  const { perfil } = useAuth()
  const destino = perfil ? '/negocio' : '/cadastro'
  return (
    <Pagina>
      <section className="py-8 sm:py-12">
        <p className="text-sm font-bold text-primary">Para salões, barbearias e outros serviços</p>
        <h1 className="mt-2 max-w-3xl text-4xl font-black leading-tight tracking-tight sm:text-5xl">
          Encha os horários vagos sem passar o dia no WhatsApp.
        </h1>
        <p className="mt-5 max-w-2xl text-lg leading-relaxed text-muted">
          Publique os horários livres e quem está procurando perto reserva na hora. Grátis durante os testes, sem
          mensalidade e sem taxa por reserva.
        </p>
        <div className="mt-8 flex flex-col gap-3 sm:flex-row">
          <Link to={destino} className={estilos.primario}>{perfil ? 'Ir para Meu negócio' : 'Cadastrar meu negócio grátis'}</Link>
          <Link to="/" className={estilos.secundario}>Ver como o cliente vê</Link>
        </div>
      </section>

      <section aria-labelledby="vantagens" className="border-t border-border py-10">
        <h2 id="vantagens" className="text-2xl font-bold">O que muda no seu dia</h2>
        <ul className="mt-6 grid gap-4 sm:grid-cols-2">
          {VANTAGENS.map(({ icone: Icone, titulo, texto }) => (
            <li key={titulo} className={estilos.cartao}>
              <Icone className="h-6 w-6 text-primary" aria-hidden="true" />
              <h3 className="mt-3 font-bold">{titulo}</h3>
              <p className="mt-1 text-sm leading-relaxed text-muted">{texto}</p>
            </li>
          ))}
        </ul>
      </section>

      <section aria-labelledby="como-comecar" className="border-t border-border py-10">
        <h2 id="como-comecar" className="text-2xl font-bold">Como começar</h2>
        <ol className="mt-6 grid gap-4 sm:grid-cols-3">
          {PASSOS.map(({ titulo, texto }, n) => (
            <li key={titulo} className={estilos.cartao}>
              <span className="flex h-8 w-8 items-center justify-center rounded-full bg-primary font-bold text-background" aria-hidden="true">{n + 1}</span>
              <h3 className="mt-3 font-bold">{titulo}</h3>
              <p className="mt-1 text-sm leading-relaxed text-muted">{texto}</p>
            </li>
          ))}
        </ol>
      </section>

      <section aria-labelledby="perguntas" className="border-t border-border py-10">
        <h2 id="perguntas" className="text-2xl font-bold">Perguntas frequentes</h2>
        <div className="mt-6 divide-y divide-border rounded-2xl border border-border bg-surface">
          {PERGUNTAS.map(([p, r]) => (
            <details key={p} className="group p-5">
              <summary className="cursor-pointer list-none font-semibold marker:hidden">
                <span className="flex items-center justify-between gap-4">{p}<span className="text-primary group-open:rotate-45" aria-hidden="true">+</span></span>
              </summary>
              <p className="mt-2 leading-relaxed text-muted">{r}</p>
            </details>
          ))}
        </div>
        <p className="mt-8 flex items-start gap-2 text-sm text-muted">
          <ShieldCheck className="h-5 w-5 shrink-0 text-primary" aria-hidden="true" />
          O Agora aproxima você de quem procura um horário. O atendimento, o preço e o pagamento continuam combinados entre
          você e o cliente.
        </p>
        <div className="mt-8 flex flex-col gap-3 sm:flex-row">
          <Link to={destino} className={estilos.primario}>
            <Megaphone className="h-4 w-4" aria-hidden="true" /> {perfil ? 'Ir para Meu negócio' : 'Cadastrar meu negócio grátis'}
          </Link>
          <Link to="/ajuda" className={estilos.secundario}>Central de ajuda</Link>
        </div>
      </section>
    </Pagina>
  )
}
