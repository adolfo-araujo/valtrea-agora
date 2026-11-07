import { Link } from 'react-router-dom'
import { Pagina, estilos } from '../components/ui'
import { LEGAL } from '../lib/legal'

// Central de ajuda: perguntas frequentes do Valtrea Agora. Edite os textos aqui.
const SECOES = [
  [
    "Para quem procura um horário",
    [
      [
        "Como reservo um horário?",
        "Escolha o estado, procure pela cidade, bairro ou nome, abra o estabelecimento e toque no horário. A reserva é confirmada na hora e você recebe um e-mail."
      ],
      [
        "Pago pelo app?",
        "Não. O Agora não recebe pagamentos: você paga direto no estabelecimento. Desconfie de quem pedir Pix ou sinal para garantir um horário e denuncie."
      ],
      [
        "Como lembro da reserva?",
        "Em Minhas reservas, toque em \"Adicionar à agenda\" (ou Google Agenda). A agenda do celular avisa 1 dia e 2 horas antes."
      ],
      [
        "Posso ter quantas reservas?",
        "Até 3 marcadas ao mesmo tempo. Se não puder ir, cancele em Minhas reservas para liberar o horário para outra pessoa."
      ],
      [
        "O estabelecimento cancelou minha reserva.",
        "Você recebe um e-mail avisando. Procure outro horário no mesmo lugar ou em outro perto."
      ]
    ]
  ],
  [
    "Para quem tem um negócio",
    [
      [
        "Como cadastro meu negócio?",
        "Crie a conta, vá em Meu negócio e cadastre o estabelecimento. Ele passa por análise antes de aparecer na busca. Veja todos os detalhes na página Para negócios."
      ],
      [
        "Como publico os horários?",
        "Na agenda do estabelecimento, use \"Toda semana\" para publicar de uma vez (dias, horário de abrir e fechar e intervalo) ou \"Um dia\" para horários avulsos. Até 300 horários vagos ao mesmo tempo."
      ],
      [
        "Como divulgo minha página?",
        "Depois da aprovação, a agenda mostra \"Divulgue sua página\": link para a bio do Instagram, botão do WhatsApp e QR Code para imprimir e deixar no balcão."
      ]
    ]
  ],
  [
    "Conta e dados",
    [
      [
        "O site demorou para abrir. É normal?",
        "Quando o serviço fica um tempo sem uso, a primeira ação pode levar até 1 minuto, porque o servidor estava em repouso. Aparece o aviso \"Acordando o servidor\" e, depois disso, fica rápido."
      ],
      [
        "Não recebi o e-mail de confirmação.",
        "Confira a caixa de spam e as abas Promoções ou Atualizações. Se não estiver lá, entre na conta e peça um novo código na tela de confirmação. O código vale pelo tempo informado no e-mail."
      ],
      [
        "Esqueci a senha.",
        "Na tela de entrar, toque em \"Esqueci a senha\" e informe seu e-mail. Você recebe um código para criar uma senha nova; ele vale pelo tempo informado no e-mail."
      ],
      [
        "Como baixo meus dados ou excluo a conta?",
        "Em Conta você baixa uma cópia de tudo o que guardamos sobre você e pode excluir a conta. A exclusão apaga seus dados na hora, exceto o que a lei manda guardar por um tempo (veja a Política de privacidade)."
      ],
      [
        "Dá para usar como app no celular?",
        "Sim. No Android, abra o site no Chrome e toque em \"Instalar app\" (ou baixe o app quando estiver na Play Store). No iPhone, abra no Safari, toque em Compartilhar e depois em \"Adicionar à Tela de Início\"."
      ]
    ]
  ]
]

export function AjudaPage() {
  return (
    <Pagina titulo="Central de ajuda" largura="max-w-3xl">
      <p className="text-muted">Respostas rápidas para as dúvidas mais comuns sobre o Valtrea Agora.</p>
      {SECOES.map(([titulo, perguntas]) => (
        <section key={titulo} aria-labelledby={`sec-${titulo}`} className="mt-8">
          <h2 id={`sec-${titulo}`} className="text-xl font-bold">{titulo}</h2>
          <div className="mt-3 divide-y divide-border rounded-2xl border border-border bg-surface">
            {perguntas.map(([p, r]) => (
              <details key={p} className="group p-5">
                <summary className="cursor-pointer list-none font-semibold marker:hidden">
                  <span className="flex items-center justify-between gap-4">{p}<span className="text-primary group-open:rotate-45" aria-hidden="true">+</span></span>
                </summary>
                <p className="mt-2 leading-relaxed text-muted">{r}</p>
              </details>
            ))}
          </div>
        </section>
      ))}
      <section className="mt-10 rounded-2xl border border-border bg-surface p-5">
        <h2 className="font-bold">Não achou a resposta?</h2>
        <p className="mt-1 text-sm text-muted">
          Escreva para <a href={`mailto:${LEGAL.email}`} className={estilos.link}>{LEGAL.email}</a>. Respondemos por e-mail.
          Veja também os <Link to="/termos" className={estilos.link}>Termos de uso</Link> e a{' '}
          <Link to="/privacidade" className={estilos.link}>Política de privacidade</Link>.
        </p>
      </section>
    </Pagina>
  )
}
