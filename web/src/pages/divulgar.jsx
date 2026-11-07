import { useState } from 'react'
import { Copy, Download, MessageCircle } from 'lucide-react'
import { useQrCode } from '../components/qr'
import { estilos } from '../components/ui'

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

  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(url)
      setCopiado(true)
      setTimeout(() => setCopiado(false), 2500)
    } catch {
      window.prompt('Copie o link:', url)
    }
  }

  return (
    <div className="grid items-start gap-5 sm:grid-cols-[1fr_auto]">
      <div>
        <p className="text-sm text-muted">
          Coloque o QR Code no balcão ou no espelho e o link na bio do Instagram e no WhatsApp. O cliente vê os horários
          vagos e reserva na hora, sem precisar ligar.
        </p>
        <label htmlFor="link-divulgar" className="mt-4 block text-sm font-semibold">Link da sua página</label>
        <input id="link-divulgar" readOnly value={url} onFocus={(e) => e.target.select()}
          className="mt-1 h-11 w-full rounded-lg border border-border bg-background px-3 text-foreground" />
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
        </div>
        <span className="sr-only" aria-live="polite">{copiado ? 'Link copiado' : ''}</span>
      </div>
      {qr ? (
        <img src={qr} alt={`QR Code da página de ${est.nome} no Valtrea Agora`} className="h-40 w-40 rounded-lg bg-white p-1" />
      ) : (
        <div className="h-40 w-40 animate-pulse rounded-lg bg-surface-2" aria-hidden="true" />
      )}
    </div>
  )
}
