// Cliente da API do Valtrea Agora. O token de acesso dura 15 minutos e é renovado sozinho com o token
// de sessão (cada token de sessão vale uma vez).
const BASE = `${(import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '')}/api/v1`

const KEY = 'valtrea-agora:sessao'

export const sessao = {
  ler() {
    try {
      return JSON.parse(localStorage.getItem(KEY)) || null
    } catch {
      return null
    }
  },
  salvar(tokens) {
    try {
      localStorage.setItem(KEY, JSON.stringify({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken }))
    } catch {
      // navegação privada sem armazenamento: a sessão dura até fechar a aba
    }
    memoria = tokens
  },
  limpar() {
    try {
      localStorage.removeItem(KEY)
    } catch {
      // nada a fazer
    }
    // Apaga a cópia de "Minhas reservas" guardada para uso sem internet (public/sw.js)
    if (typeof caches !== 'undefined') caches.delete('valtrea-dados').catch(() => {})
    memoria = null
  },
}

// Pedidos que passaram de 4 s: mostram o aviso de "servidor acordando" (veja AvisoServidorLento).
let lentos = 0
const marcarLento = (delta) => {
  lentos = Math.max(0, lentos + delta)
  window.dispatchEvent(new CustomEvent('valtrea:lento', { detail: lentos }))
}

// Acorda a API (plano grátis do Render) assim que o site abre, enquanto a pessoa ainda lê ou digita.
if (import.meta.env.PROD) {
  fetch(`${BASE.replace(/\/api\/v1$/, '')}/actuator/health`, { mode: 'no-cors', cache: 'no-store' }).catch(() => {})
}

let memoria = sessao.ler()
let renovando = null

export class ErroApi extends Error {
  constructor(status, mensagem, campos) {
    super(mensagem)
    this.status = status
    this.campos = campos
  }
}

async function renovar() {
  const refreshToken = memoria?.refreshToken
  if (!refreshToken) throw new ErroApi(401, 'Sua sessão expirou. Entre novamente.')
  const res = await fetch(`${BASE}/auth/renovar`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  if (!res.ok) {
    sessao.limpar()
    window.dispatchEvent(new Event('agora:saiu'))
    throw new ErroApi(401, 'Sua sessão expirou. Entre novamente.')
  }
  sessao.salvar(await res.json())
}

async function mensagemDe(res) {
  const corpo = await res.json().catch(() => ({}))
  const primeiroCampo = corpo.campos && Object.entries(corpo.campos)[0]
  const texto = primeiroCampo ? `${primeiroCampo[1]}` : corpo.detail || corpo.message
  if (res.status === 429) return new ErroApi(429, texto || 'Muitas tentativas. Aguarde alguns minutos.')
  // Mensagens que dizem o que fazer, quando o servidor não explica
  if (res.status >= 500) return new ErroApi(res.status, 'Tivemos um problema do nosso lado. Tente de novo em alguns minutos; se continuar, conte para nós em "Enviar sugestão", no rodapé.')
  if (!texto && res.status === 404) return new ErroApi(404, 'Não encontramos o que você procurou. O link pode estar incompleto ou o item foi apagado.')
  if (!texto && res.status === 403) return new ErroApi(403, 'Você não tem acesso a isto. Confira se entrou com a conta certa.')
  return new ErroApi(res.status, texto || 'Não foi possível concluir agora. Tente de novo em instantes.', corpo.campos)
}

export async function chamar(metodo, rota, corpo, { autenticar = true, tentativa = 0 } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (autenticar && memoria?.accessToken) headers.Authorization = `Bearer ${memoria.accessToken}`
  let res
  let avisou = false
  const relogio = setTimeout(() => {
    avisou = true
    marcarLento(1)
  }, 4000)
  try {
    res = await fetch(`${BASE}${rota}`, { method: metodo, headers, body: corpo ? JSON.stringify(corpo) : undefined })
  } catch {
    throw new ErroApi(0, 'Sem conexão com o servidor. Confira a internet e tente de novo. Na primeira vez do dia, ele pode levar até 1 minuto para acordar.')
  } finally {
    clearTimeout(relogio)
    if (avisou) marcarLento(-1)
  }
  if (res.status === 401 && autenticar && tentativa === 0 && memoria?.refreshToken) {
    renovando ??= renovar().finally(() => (renovando = null))
    await renovando
    return chamar(metodo, rota, corpo, { autenticar, tentativa: 1 })
  }
  if (!res.ok) throw await mensagemDe(res)
  if (res.status === 204 || res.status === 202) return null
  const tipo = res.headers.get('Content-Type') || ''
  return tipo.includes('json') ? res.json() : res.text()
}

export const estaLogado = () => Boolean(memoria?.refreshToken)
export const tokenDeSessao = () => memoria?.refreshToken

export const TIPOS = {
  SALAO: 'Salão de beleza',
  BARBEARIA: 'Barbearia',
  MANICURE: 'Manicure e pedicure',
  ESTETICA: 'Estética',
  SOBRANCELHA: 'Sobrancelha e cílios',
  MASSAGEM: 'Massagem',
  PET: 'Banho e tosa (pet)',
  OUTRO: 'Outro serviço',
}

export const STATUS_RESERVA = {
  CONFIRMADA: 'Confirmada',
  CANCELADA_CLIENTE: 'Cancelada por você',
  CANCELADA_ESTABELECIMENTO: 'Cancelada pelo estabelecimento',
  ATENDIDA: 'Atendida',
  NAO_COMPARECEU: 'Não compareceu',
}

export const STATUS_ESTABELECIMENTO = {
  EM_ANALISE: 'Em análise',
  APROVADO: 'Aprovado: aparece na busca',
  RECUSADO: 'Precisa de ajuste',
}

export const MOTIVOS = {
  FALSO: 'O lugar não existe ou não é o que diz',
  NAO_RESPEITA: 'Não respeita as reservas',
  GOLPE: 'Pede pagamento antecipado, Pix ou dados',
  IMPROPRIO: 'Nome, descrição ou serviço impróprio',
  OUTRO: 'Outro motivo',
}

export const UFS = ['AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE',
  'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO']

const dia = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' })
const hora = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' })
const chaveDia = (d) => `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`

export const horaDe = (iso) => hora.format(new Date(iso))
export const dataCurta = (iso) => new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })

/** "Hoje", "Amanhã" ou "quinta-feira, 12 de novembro". */
export function nomeDoDia(iso) {
  const d = new Date(iso)
  const hoje = new Date()
  const amanha = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() + 1)
  if (chaveDia(d) === chaveDia(hoje)) return 'Hoje'
  if (chaveDia(d) === chaveDia(amanha)) return 'Amanhã'
  return dia.format(d)
}

export const quando = (iso) => `${nomeDoDia(iso)}, ${horaDe(iso)}`

/** Agrupa uma lista ordenada por {@code inicio} em [[nomeDoDia, itens], ...]. */
export function porDia(lista) {
  const grupos = []
  for (const item of lista) {
    const nome = nomeDoDia(item.inicio)
    if (grupos.length === 0 || grupos[grupos.length - 1][0] !== nome) grupos.push([nome, []])
    grupos[grupos.length - 1][1].push(item)
  }
  return grupos
}

export const preco = (centavos) =>
  centavos == null ? null : (centavos / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
