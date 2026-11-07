// "Adicionar à agenda": arquivo de calendário (.ics) com dois lembretes (1 dia e 2 horas antes) e link do Google Agenda.
// Tudo montado no navegador: funciona como lembrete grátis, sem precisar do servidor.

const doisDigitos = (n) => String(n).padStart(2, '0')
const utc = (d) =>
  `${d.getUTCFullYear()}${doisDigitos(d.getUTCMonth() + 1)}${doisDigitos(d.getUTCDate())}T` +
  `${doisDigitos(d.getUTCHours())}${doisDigitos(d.getUTCMinutes())}${doisDigitos(d.getUTCSeconds())}Z`

// Texto seguro dentro do .ics (RFC 5545): escapa \ ; , e quebras de linha
const ics = (texto) => String(texto ?? '').replace(/\\/g, '\\\\').replace(/;/g, '\\;').replace(/,/g, '\\,').replace(/\r?\n/g, '\\n')

// Linhas de no máximo 75 caracteres, continuando com um espaço (RFC 5545)
const dobrar = (linha) => {
  const partes = []
  let resto = linha
  while (resto.length > 75) {
    partes.push(resto.slice(0, 75))
    resto = ` ${resto.slice(75)}`
  }
  partes.push(resto)
  return partes.join('\r\n')
}

const dadosDa = (r) => {
  const inicio = new Date(r.inicio)
  const fim = new Date(inicio.getTime() + r.duracaoMin * 60000)
  const e = r.estabelecimento
  const titulo = `${r.servico} · ${e.nome}`
  const local = `${e.endereco}, ${e.bairro}, ${e.cidade} - ${e.uf}`
  const detalhes = `Reserva feita pelo Valtrea Agora.\nTelefone do estabelecimento: ${e.telefone}\nPara cancelar: ${window.location.origin}/reservas`
  return { inicio, fim, titulo, local, detalhes }
}

export function baixarIcs(r) {
  const { inicio, fim, titulo, local, detalhes } = dadosDa(r)
  const linhas = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Valtrea//Agora//PT-BR',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH',
    'BEGIN:VEVENT',
    `UID:reserva-${r.id}@agora.valtrea.com.br`,
    `DTSTAMP:${utc(new Date())}`,
    `DTSTART:${utc(inicio)}`,
    `DTEND:${utc(fim)}`,
    `SUMMARY:${ics(titulo)}`,
    `LOCATION:${ics(local)}`,
    `DESCRIPTION:${ics(detalhes)}`,
    'BEGIN:VALARM',
    'ACTION:DISPLAY',
    `DESCRIPTION:${ics(`Amanhã: ${titulo}`)}`,
    'TRIGGER:-P1D',
    'END:VALARM',
    'BEGIN:VALARM',
    'ACTION:DISPLAY',
    `DESCRIPTION:${ics(`Daqui a 2 horas: ${titulo}`)}`,
    'TRIGGER:-PT2H',
    'END:VALARM',
    'END:VEVENT',
    'END:VCALENDAR',
  ]
  const blob = new Blob([`${linhas.map(dobrar).join('\r\n')}\r\n`], { type: 'text/calendar;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'reserva-valtrea-agora.ics'
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function linkGoogleAgenda(r) {
  const { inicio, fim, titulo, local, detalhes } = dadosDa(r)
  const q = new URLSearchParams({ action: 'TEMPLATE', text: titulo, dates: `${utc(inicio)}/${utc(fim)}`, location: local, details: detalhes })
  return `https://calendar.google.com/calendar/render?${q.toString()}`
}
