import { Link } from 'react-router-dom'
import { LEGAL } from '../lib/legal'
import { Pagina, estilos } from '../components/ui'

const Email = () => <a href={`mailto:${LEGAL.email}`} className={estilos.link}>{LEGAL.email}</a>

function Documento({ titulo, children }) {
  return (
    <Pagina titulo={titulo} largura="max-w-3xl">
      <p className="mb-6 text-sm text-muted">Atualizado em {LEGAL.updatedAt}.</p>
      <div className="space-y-4 leading-relaxed [&_h2]:mt-8 [&_h2]:text-xl [&_h2]:font-bold [&_li]:ml-5 [&_li]:list-disc [&_td]:border [&_td]:border-border [&_td]:p-2 [&_th]:border [&_th]:border-border [&_th]:bg-surface-2 [&_th]:p-2 [&_th]:text-left">
        {children}
      </div>
    </Pagina>
  )
}

function Tabela({ cabecalho, linhas }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-sm">
        <thead><tr>{cabecalho.map((c) => <th key={c} scope="col">{c}</th>)}</tr></thead>
        <tbody>{linhas.map((l) => <tr key={l[0]}>{l.map((c, i) => <td key={i}>{c}</td>)}</tr>)}</tbody>
      </table>
    </div>
  )
}

export function PrivacidadePage() {
  return (
    <Documento titulo="Política de privacidade">
      <p>
        Esta Política explica como o {LEGAL.appName} (site {LEGAL.site} e aplicativo Android) trata dados pessoais,
        conforme a Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018, "LGPD").
      </p>

      <h2>1. Quem é o responsável</h2>
      <p>
        O controlador é {LEGAL.controller} ({LEGAL.controllerType}), {LEGAL.forum}. Canal para qualquer assunto sobre os
        seus dados: <Email />. Como agente de tratamento de pequeno porte, não indicamos encarregado (Resolução CD/ANPD nº
        2/2022); o canal é respondido diretamente pelo responsável.
      </p>

      <h2>2. Dados que tratamos, para quê e com qual base legal</h2>
      <Tabela cabecalho={['Dado', 'Para quê', 'Base legal (LGPD)']} linhas={[
        ['Seu nome, e-mail e senha (guardada só como hash)', 'Criar a conta, entrar e mandar os e-mails do serviço (códigos, reservas e cancelamentos)', 'Execução de contrato (art. 7º, V)'],
        ['Suas reservas: horário, serviço e, se você quiser, um telefone e uma observação', 'Guardar o horário para você e deixar o estabelecimento falar com você sobre a reserva', 'Execução de contrato (art. 7º, V)'],
        ['Se você tem um negócio: nome, endereço, telefone e descrição do estabelecimento; horários publicados', 'Mostrar o estabelecimento e os horários vagos a quem procura', 'Execução de contrato (art. 7º, V)'],
        ['Presença nas reservas (atendida ou não compareceu), marcada pelo estabelecimento', 'Organizar a agenda do estabelecimento', 'Execução de contrato (art. 7º, V)'],
        ['Suas denúncias (qual estabelecimento e o motivo)', 'Revisar estabelecimentos que podem ser falsos ou golpes', 'Legítimo interesse (art. 7º, IX)'],
        ['Data e versão do aceite destes documentos', 'Comprovar o aceite', 'Exercício regular de direitos (art. 7º, VI)'],
        ['Registros de erro (sem nome, e-mail, IP nem conteúdo)', 'Corrigir falhas e lentidão', 'Legítimo interesse (art. 7º, IX)'],
        ['Endereço IP, data e hora', 'Limitar tentativas e bloquear abusos; o IP fica só na memória do servidor', 'Legítimo interesse (art. 7º, IX)'],
      ]} />
      <p>
        <strong>O que é público:</strong> os dados dos estabelecimentos aprovados (nome, tipo, endereço, telefone e
        descrição, que são do negócio) e os horários vagos. <strong>O estabelecimento vê de quem reservou</strong> só o
        primeiro nome e, se você informar, o telefone e a observação. Ele nunca vê o seu e-mail nem as suas outras
        reservas. <strong>A administração</strong> analisa cada estabelecimento antes de aprovar e vê o primeiro nome do
        dono e os motivos das denúncias, nunca quem denunciou. Não há pagamento pelo app: não pedimos cartão nem dados
        bancários. Não usamos localização, contatos do celular, fotos, anúncios nem ferramentas de rastreamento. No
        navegador, guardamos só o necessário para manter você conectado e o último filtro da busca.
      </p>

      <h2>3. Com quem compartilhamos</h2>
      <Tabela cabecalho={['Fornecedor', 'Para quê', 'Onde']} linhas={[
        ['Render', 'Servidor da API', 'Estados Unidos'],
        ['Neon', 'Banco de dados', 'Estados Unidos (Virgínia), na mesma região do servidor'],
        ['Vercel', 'Hospedagem do site', 'Estados Unidos e rede global'],
        ['Resend', 'Envio dos e-mails (códigos, reservas e cancelamentos)', 'São Paulo, Brasil'],
        ['Sentry', 'Registro de erros, sem dados pessoais', 'Estados Unidos'],
        ['GitHub', 'Cópia de segurança diária do banco, criptografada', 'Estados Unidos'],
        ['ImprovMX e Google (Gmail)', 'Recebimento das mensagens enviadas ao nosso e-mail de contato', 'Estados Unidos'],
        ['Google Play', 'Distribuição do app; segue a política de privacidade do Google', 'Estados Unidos'],
      ]} />
      <p>
        Também podemos compartilhar dados quando exigido por lei ou ordem de autoridade competente. Não vendemos dados
        pessoais. Parte dos fornecedores fica fora do Brasil; essas transferências acontecem só para o serviço funcionar,
        com fornecedores que adotam medidas de segurança adequadas (art. 33 da LGPD).
      </p>

      <h2>4. Por quanto tempo guardamos</h2>
      <Tabela cabecalho={['Dado', 'Prazo']} linhas={[
        ['Conta e estabelecimentos', 'Enquanto a conta existir. Ao excluir a conta, tudo é apagado na hora'],
        ['Horários e reservas (com telefone e observação)', '90 dias depois do horário'],
        ['Estabelecimento recusado e não corrigido', '30 dias depois da recusa'],
        ['Denúncias', 'Até a administração decidir'],
        ['Cadastros nunca confirmados e sem estabelecimento', '30 dias'],
        ['Códigos enviados por e-mail e sessões', 'Até vencer, mais 1 dia'],
        ['Cópias de segurança criptografadas', '30 dias'],
        ['Registros de erro', 'Prazo do plano do Sentry, até 90 dias'],
      ]} />

      <h2 id="direitos">5. Seus direitos</h2>
      <p>
        Pela LGPD (art. 18), você pode pedir confirmação e acesso, correção, anonimização, bloqueio ou eliminação,
        portabilidade, informação sobre compartilhamento e revogação do consentimento. Em <strong>Conta</strong>, você
        corrige o nome, baixa uma cópia completa dos seus dados (<strong>Baixar meus dados</strong>) e exclui a conta. Você
        pode cancelar uma reserva e apagar um estabelecimento a qualquer momento. Respondemos em até 15 dias. Você também
        pode reclamar à Autoridade Nacional de Proteção de Dados (ANPD).
      </p>

      <h2>6. Segurança</h2>
      <p>
        HTTPS sempre; senhas com bcrypt; códigos por e-mail com validade curta e poucas tentativas; bloqueio após senhas erradas; limites de
        pedidos por IP; e cópia de segurança diária criptografada. Se acontecer um incidente que possa causar risco
        relevante, avisaremos você e a ANPD.
      </p>

      <h2>7. Idade mínima e mudanças</h2>
      <p>
        O {LEGAL.appName} é destinado a maiores de 18 anos. Quando esta Política mudar, atualizaremos a data no topo e
        avisaremos no site, no app ou por e-mail se a mudança for importante.
      </p>
    </Documento>
  )
}

export function TermosPage() {
  return (
    <Documento titulo="Termos de uso">
      <p>
        Estes Termos regulam o uso do {LEGAL.appName} (site {LEGAL.site}, aplicativo Android e API), mantido por{' '}
        {LEGAL.controller} ({LEGAL.controllerType}), {LEGAL.forum}. Ao criar uma conta, você declara que leu e aceita
        estes Termos e que está ciente da <Link to="/privacidade" className={estilos.link}>Política de privacidade</Link>.
      </p>

      <h2>1. O que é o Valtrea Agora</h2>
      <p>
        Um serviço que mostra horários vagos de salões, barbearias e outros serviços da sua região e permite reservá-los.
        Estabelecimentos publicam os horários; clientes reservam. O serviço está em fase de testes e é gratuito para os
        dois lados.
      </p>
      <p>
        <strong>O Valtrea Agora não presta os serviços nem recebe pagamentos.</strong> A reserva só guarda o horário. O
        serviço, o preço e o pagamento são combinados e feitos diretamente com o estabelecimento, que é o responsável por
        eles. Os preços mostrados são informados pelo estabelecimento.
      </p>

      <h2>2. Conta</h2>
      <ul>
        <li>É preciso ter 18 anos ou mais e informar dados verdadeiros.</li>
        <li>Você é responsável pela sua senha e pelo que for feito na sua conta.</li>
        <li>Para reservar, cadastrar um estabelecimento e denunciar é preciso confirmar o e-mail.</li>
        <li>Cada pessoa pode ter até 3 reservas marcadas ao mesmo tempo. Se não puder ir, cancele antes do horário.</li>
        <li>Quem cadastra um estabelecimento declara que o representa e que os dados são verdadeiros. Todo cadastro passa
          por análise antes de aparecer.</li>
      </ul>

      <h2>3. Uso permitido</h2>
      <p>Você concorda em não:</p>
      <ul>
        <li>cadastrar estabelecimento falso, de outra pessoa ou que não presta o serviço anunciado;</li>
        <li>pedir pagamento antecipado, Pix, sinal ou dados bancários para garantir uma reserva;</li>
        <li>reservar horários sem a intenção de ir, ou reservar para outras pessoas sem que elas saibam;</li>
        <li>usar o telefone ou o nome de quem reservou para outra coisa que não seja a reserva;</li>
        <li>publicar conteúdo ofensivo, discriminatório ou serviços ilegais;</li>
        <li>tentar acessar dados que não são seus, explorar falhas ou sobrecarregar o serviço.</li>
      </ul>

      <h2>4. Análise, denúncias e cancelamentos</h2>
      <p>
        Todo estabelecimento é analisado antes de aparecer e pode ser recusado, com o motivo enviado por e-mail. Qualquer
        cliente pode denunciar um estabelecimento; com três denúncias de pessoas diferentes, ele sai da busca até nova
        análise. Cliente e estabelecimento podem cancelar uma reserva, e o outro lado recebe um e-mail. Podemos suspender
        ou encerrar contas que violem estes Termos, avisando o motivo pelo e-mail da conta. Não concorda com uma decisão?
        Escreva para <Email />.
      </p>
      <p>
        <strong>Segurança:</strong> desconfie de quem pedir pagamento antecipado para garantir um horário e denuncie.
      </p>

      <h2>5. Disponibilidade e mudanças</h2>
      <p>
        Por ser uma versão de testes em hospedagem gratuita, o serviço pode demorar a responder ou ficar fora do ar, e
        funções podem mudar. Mudanças relevantes serão avisadas no site, no app ou por e-mail.
      </p>

      <h2>6. Responsabilidades</h2>
      <p>
        Os dados dos estabelecimentos, os horários e os serviços são de responsabilidade de quem os publicou. Removemos
        conteúdo ilegal quando somos avisados, nos termos do Marco Civil da Internet (Lei nº 12.965/2014). Nada nestes Termos limita os direitos que
        você tem como consumidor pela legislação brasileira.
      </p>

      <h2>7. Encerramento, alterações e foro</h2>
      <p>
        Você pode excluir a conta a qualquer momento em <strong>Conta → Excluir conta</strong>. Se não concordar com uma
        nova versão destes Termos, pode excluir a conta sem custo. Estes Termos seguem as leis do Brasil; fica eleito o foro
        de {LEGAL.forum}, ressalvado o direito do consumidor de propor ação no foro do seu domicílio. Contato: <Email />.
      </p>
    </Documento>
  )
}

export function AcessibilidadePage() {
  return (
    <Documento titulo="Declaração de acessibilidade">
      <p>
        Queremos que o {LEGAL.appName} possa ser usado por todas as pessoas, incluindo pessoas com deficiência, como prevê a
        Lei Brasileira de Inclusão (Lei nº 13.146/2015, art. 63). Buscamos atender às WCAG 2.2, nível AA, e à ABNT NBR 17225.
      </p>
      <h2>O que já fizemos</h2>
      <ul>
        <li>As principais páginas são verificadas automaticamente (axe-core, WCAG A e AA) a cada mudança, no computador e no celular.</li>
        <li>Navegação completa pelo teclado, foco visível e link “Pular para o conteúdo”.</li>
        <li>Campos com rótulo visível e mensagens de erro em texto, ligadas ao campo.</li>
        <li>Contraste revisado no tema escuro; o zoom do navegador e do celular funciona em todas as páginas.</li>
        <li>No app Android, telas com os componentes do Material 3, que funcionam com o TalkBack.</li>
      </ul>
      <h2>Limitações conhecidas</h2>
      <ul>
        <li>Ainda não fizemos testes com pessoas que usam tecnologias assistivas, e não há tradução para Libras.</li>
      </ul>
      <h2>Encontrou uma barreira?</h2>
      <p>Escreva para <Email /> contando a página e o problema. Respondemos o quanto antes.</p>
    </Documento>
  )
}

export function ExcluirContaPage() {
  return (
    <Documento titulo="Como excluir a sua conta">
      <p>
        Entre no {LEGAL.appName} (site ou app) e vá em <strong>Conta → Excluir conta</strong>, confirmando com a sua senha.
        A conta, as suas reservas, os seus estabelecimentos (com os horários e as reservas deles) e as suas denúncias são
        apagados na hora.
      </p>
      <p>
        Sem acesso à conta? Use <Link to="/esqueci-senha" className={estilos.link}>Esqueci minha senha</Link> ou escreva
        para <Email /> com o assunto “Excluir conta”, a partir do e-mail cadastrado. Respondemos em até 15 dias.
      </p>
      <p>Cópias de segurança criptografadas são apagadas em até 30 dias, sem nenhum uso nesse período.</p>
    </Documento>
  )
}
