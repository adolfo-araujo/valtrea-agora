// Teste de carga do Valtrea Agora (k6: https://k6.io). Rode pelo rodar-teste-de-carga.bat.
import http from 'k6/http'
import { check } from 'k6'
import { API, entrar, espera, ipDe, resumo } from './comum.js'

const USERS = Number(__ENV.USERS || 100)
export const options = {
    setupTimeout: '10m',
    scenarios: {
        uso: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: __ENV.RAMP || '1m', target: USERS },
                { duration: __ENV.HOLD || '3m', target: USERS },
                { duration: '20s', target: 0 },
            ],
            gracefulRampDown: '15s',
        },
    },
    // Mesmo critério do Valtrea Finance e do Pay.
    thresholds: {
        http_req_failed: ['rate<0.01'],
        'http_req_duration{tipo:leitura}': ['p(95)<1000'],
        'http_req_duration{tipo:escrita}': ['p(95)<1000'],
        'http_req_duration{tipo:login}': ['p(95)<2000'],
        checks: ['rate>0.99'],
    },
    summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
}

let token = null
const conta = () => (__VU - 1) % USERS
const ip = () => ipDe(__VU + 1000)
const p = (tipo, rota, extra = {}) => ({
    headers: { 'Content-Type': 'application/json', 'CF-Connecting-IP': ip(), Authorization: `Bearer ${token}` },
    tags: { tipo, rota },
    timeout: '60s',
    ...extra,
})
const ok = (r) => r.status >= 200 && r.status < 300

const DONOS = 5

// Antes do teste: 5 pessoas cadastram um estabelecimento cada, com 300 horários vagos nos próximos 6 dias,
// e a conta carga-0 (administradora no teste) aprova todos.
export function setup() {
    const ipSetup = ipDe(999999)
    const cab = (t) => ({ headers: { 'Content-Type': 'application/json', 'CF-Connecting-IP': ipSetup, Authorization: `Bearer ${t}` }, timeout: '120s' })
    const admin = entrar(0, ipSetup)
    const base = Date.now() + 2 * 3600 * 1000
    for (let d = 0; d < DONOS; d++) {
        const t = entrar(d, ipSetup)
        const e = http.post(`${API}/api/v1/estabelecimentos`, JSON.stringify({
            nome: `Barbearia Carga ${d}`, tipo: 'BARBEARIA', descricao: 'Criada pelo teste de carga',
            endereco: `Rua do Teste, ${100 + d}`, bairro: 'Centro', cidade: 'Cidade Carga', uf: 'RJ', telefone: '(21) 99999-0000',
        }), cab(t))
        if (e.status !== 201) throw new Error(`estabelecimento ${d}: ${e.status} ${e.body}`)
        const id = e.json('id')
        for (let lote = 0; lote < 15; lote++) {
            const inicios = []
            for (let k = 0; k < 20; k++) inicios.push(new Date(base + ((lote * 20 + k) * 25 + d) * 60 * 1000).toISOString())
            const h = http.post(`${API}/api/v1/estabelecimentos/${id}/horarios`, JSON.stringify({
                servico: 'Corte', duracaoMin: 30, precoCentavos: 4500, inicios,
            }), cab(t))
            if (h.status !== 201) throw new Error(`horários ${d}/${lote}: ${h.status} ${h.body}`)
        }
        const a = http.post(`${API}/api/v1/admin/estabelecimentos/${id}/aprovar`, null, cab(admin))
        if (a.status !== 204) throw new Error(`aprovar ${d}: ${a.status} ${a.body}`)
    }
}

// Cada pessoa: entra, procura horários, "lê" por 5 a 15 s, abre um estabelecimento e às vezes reserva um
// horário e cancela em seguida. Duas pessoas no mesmo horário: uma consegue e a outra recebe 409 (esperado).
export default function () {
    if (!token) {
        token = entrar(conta(), ip())
        if (!check(token, { 'login ok': (t) => !!t })) return espera(5, 10)
    }
    const busca = http.batch([
        ['GET', `${API}/api/v1/eu`, null, p('leitura', 'GET /eu')],
        ['GET', `${API}/api/v1/publico/horarios?uf=RJ`, null, p('leitura', 'GET /publico/horarios')],
        ['GET', `${API}/api/v1/eu/reservas`, null, p('leitura', 'GET /eu/reservas')],
    ])
    check(busca, { 'busca carregou': (rs) => rs.every(ok) })
    let horarios = []
    try { horarios = busca[1].json() || [] } catch (e) { horarios = [] }
    espera(5, 15)
    if (horarios.length === 0) return
    const h = horarios[Math.floor(Math.random() * Math.min(horarios.length, 50))]
    check(http.get(`${API}/api/v1/publico/estabelecimentos/${h.estabelecimento.id}`, p('leitura', 'GET /publico/estabelecimentos/{id}')),
        { 'estabelecimento abriu': ok })
    if (Math.random() < 0.15) {
        espera(2, 5)
        const r = http.post(`${API}/api/v1/horarios/${h.id}/reserva`, JSON.stringify({}),
            p('escrita', 'POST /horarios/{id}/reserva', { responseCallback: http.expectedStatuses(200, 201, 409) }))
        check(r, { 'reserva ok ou horário já ocupado': (x) => ok(x) || x.status === 409 })
        if (ok(r)) {
            espera(2, 5)
            check(http.post(`${API}/api/v1/reservas/${r.json('id')}/cancelar`, null, p('escrita', 'POST /reservas/{id}/cancelar')),
                { 'reserva cancelada': ok })
        }
    }
}

export function handleSummary(data) {
    return resumo(data, 'Valtrea Agora')
}
