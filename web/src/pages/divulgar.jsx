import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Code, Copy, Download, MessageCircle, Printer } from 'lucide-react'
import { useQrCode } from '../components/qr'
import { estilos } from '../components/ui'
import { TIPOS, chamar } from '../lib/api'

const copiarTexto = async (texto, aoCopiar) => {
  try {
    await navigator.clipboard.writeText(texto)
    aoCopiar(true)
    setTimeout(() => aoCopiar(false), 2500)
  } catch {
    window.prompt('Copie:', texto)
  }
}

/** Código de um botão "Agendar horário" para colar no site do negócio. Só HTML e estilo, sem script. */
const codigoDoBotao = (url) =>
  `<a href="${url}" target="_blank" rel="noopener" style="display:inline-block;padding:12px 22px;border-radius:10px;background:#387A1E;color:#fff;font:600 16px/1.2 system-ui,sans-serif;text-decoration:none">Agendar horário</a>`

/**
 * Link e QR Code da página do estabelecimento, para o balcão, a bio do Instagram e o WhatsApp.
 * Cada cliente que reserva por aqui conhece o Agora: o próprio negócio divulga o serviço.
 */
export function Divulgar({ est }) {
  const url = `${window.location.origin}/e/${est.id}`
  const qr = useQrCode(url)
  const [copiado, setCopiado] = useState(false)
  const textoWhats = `Agende seu horário em ${est.nome} pelo Valtrea Agora: ${url}`
  const nomeArquivo = `qr-${est.nome.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'agora'}.png`

  const [codigoCopiado, setCodigoCopiado] = useState(false)
  const copiar = () => copiarTexto(url, setCopiado)
  const codigo = codigoDoBotao(url)

  return (
    <div className="grid items-start gap-5 sm:grid-cols-[1fr_auto]">
      <div>
        <p className="text-sm text-muted">
          Coloque o QR Code no balcão ou no espelho e o link na bio do Instagram e no WhatsApp. O cliente vê os horários
          vagos e reserva na hora, sem precisar ligar.
        </p>
        <label htmlFor="link-divulgar" className="mt-4 block text-sm font-semibold">Link da sua página</label>
        <input id="link-divulgar" readOnly value={url} onFocus={(e) => e.target.select()}
          className="mt-1 h-11 w-full rounded-xl border border-border bg-surface px-3 text-foreground" />
        <div className="mt-3 flex flex-wrap gap-2">
          <button type="button" className={estilos.secundario} onClick={copiar}>
            <Copy className="h-4 w-4" aria-hidden="true" /> {copiado ? 'Link copiado' : 'Copiar link'}
          </button>
          <a className={estilos.secundario} href={`https://wa.me/?text=${encodeURIComponent(textoWhats)}`} target="_blank" rel="noreferrer">
            <MessageCircle className="h-4 w-4" aria-hidden="true" /> Enviar no WhatsApp
          </a>
          {qr && (
            <a className={estilos.secundario} href={qr} download={nomeArquivo}>
              <Download className="h-4 w-4" aria-hidden="true" /> Baixar QR Code
            </a>
          )}
          <Link className={estilos.secundario} to={`/e/${est.id}/cartaz`}>
            <Printer className="h-4 w-4" aria-hidden="true" /> Cartaz para imprimir
          </Link>
        </div>
        <span className="sr-only" aria-live="polite">{copiado ? 'Link copiado' : codigoCopiado ? 'Código copiado' : ''}</span>

        <details className="mt-5 rounded-lg border border-border p-4">
          <summary className="flex cursor-pointer items-center gap-2 font-semibold">
            <Code className="h-4 w-4 text-primary" aria-hidden="true" /> Botão de agendamento para o seu site
          </summary>
          <p className="mt-2 text-sm text-muted">
            Tem site próprio? Cole este código onde quiser o botão. Quem clicar abre a sua página no Agora. Fica assim:
          </p>
          <p className="mt-3">
            <a href={url} target="_blank" rel="noopener noreferrer" style={{ display: 'inline-block', padding: '12px 22px', borderRadius: 10, background: '#387A1E', color: '#fff', font: '600 16px/1.2 system-ui,sans-serif', textDecoration: 'none' }}>
              Agendar horário
            </a>
          </p>
          <label htmlFor="codigo-botao" className="mt-3 block text-sm font-semibold">Código</label>
          <textarea id="codigo-botao" readOnly rows={4} value={codigo} onFocus={(e) => e.target.select()}
            className="mt-1 w-full rounded-xl border border-border bg-surface p-3 font-mono text-xs text-foreground" />
          <button type="button" className={`${estilos.secundario} mt-2`} onClick={() => copiarTexto(codigo, setCodigoCopiado)}>
            <Copy className="h-4 w-4" aria-hidden="true" /> {codigoCopiado ? 'Código copiado' : 'Copiar código'}
          </button>
        </details>
      </div>
      {qr ? (
        <img src={qr} alt={`QR Code da página de ${est.nome} no Valtrea Agora`} className="h-40 w-40 rounded-lg bg-white p-1" />
      ) : (
        <div className="h-40 w-40 animate-pulse rounded-lg bg-surface-2" aria-hidden="true" />
      )}
    </div>
  )
}

/** Cartaz em A4, preto e branco, para o balcão ou o espelho: nome, QR Code e o convite para agendar. */
export function CartazPage() {
  const { id } = useParams()
  const [e, setE] = useState(null)
  const [erro, setErro] = useState('')
  useEffect(() => {
    chamar('GET', `/publico/estabelecimentos/${id}`, null, { autenticar: false })
      .then((d) => setE(d.estabelecimento))
      .catch((err) => setErro(err.message))
  }, [id])
  const url = `${window.location.origin}/e/${id}`
  const qr = useQrCode(e ? url : null)

  if (!e) return <main className="p-8 text-muted">{erro || 'Carregando…'}</main>
  return (
    <main className="min-h-screen bg-white px-4 py-6 text-black print:p-0">
      <div className="mx-auto mb-6 flex max-w-[210mm] flex-wrap gap-2 print:hidden">
        <button type="button" onClick={() => window.print()}
          className="inline-flex h-11 items-center gap-2 rounded-lg bg-black px-5 font-semibold text-white">
          <Printer className="h-4 w-4" aria-hidden="true" /> Imprimir
        </button>
        <Link to="/negocio" className="inline-flex h-11 items-center rounded-lg border border-black/30 px-5 font-semibold">
          Voltar
        </Link>
      </div>
      <article className="mx-auto flex max-w-[210mm] flex-col items-center border-2 border-black p-[14mm] text-center print:border-0">
        <p className="text-lg font-bold uppercase tracking-widest">{TIPOS[e.tipo]}</p>
        <h1 className="mt-4 text-5xl font-black leading-tight">{e.nome}</h1>
        <p className="mt-10 text-3xl font-black">Agende seu horário pelo celular</p>
        <p className="mt-2 text-xl">Veja os horários vagos e reserve na hora, sem precisar ligar.</p>
        {qr && <img src={qr} alt="QR Code para agendar" className="mt-8 h-[85mm] w-[85mm]" />}
        <p className="mt-4 text-2xl font-bold">Aponte a câmera do celular</p>
        <p className="mt-1 break-all text-sm">{url}</p>
        <p className="mt-10 text-xs">Valtrea Agora · agora.valtrea.com.br</p>
      </article>
    </main>
  )
}
