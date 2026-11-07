// Service worker do Valtrea.
// - Telas (navegação): busca na rede primeiro; sem internet, usa a última versão salva.
// - Arquivos de /assets (têm hash no nome) e as fontes de /fonts: cache primeiro, porque nunca mudam.
// - Chamadas da API (outro endereço) não passam pelo cache, com uma exceção: "Minhas reservas" guarda a última
//   lista para abrir sem internet (no balcão, por exemplo). Essa cópia é apagada quando a pessoa sai da conta.
const CACHE = 'valtrea-v1'
const DADOS = 'valtrea-dados'
// O perfil vai junto: sem ele, o app acharia que a pessoa saiu da conta
const GUARDAR_SEM_INTERNET = ['/api/v1/eu', '/api/v1/eu/reservas']

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.add('/')))
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE && k !== DADOS).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (event) => {
  const { request } = event
  const url = new URL(request.url)

  if (request.method === 'GET' && url.origin !== self.location.origin && GUARDAR_SEM_INTERNET.some((rota) => url.pathname.endsWith(rota))) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response.ok) {
            const copy = response.clone()
            caches.open(DADOS).then((cache) => cache.put(url.href, copy))
          }
          return response
        })
        .catch(() => caches.open(DADOS).then((cache) => cache.match(url.href)).then((salva) => salva || Response.error())),
    )
    return
  }

  if (request.method !== 'GET' || url.origin !== self.location.origin) return
  if (url.pathname.startsWith('/api/')) return

  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const copy = response.clone()
          caches.open(CACHE).then((cache) => cache.put('/', copy))
          return response
        })
        .catch(() => caches.match('/')),
    )
    return
  }

  if (url.pathname.startsWith('/assets/') || url.pathname.startsWith('/fonts/')) {
    event.respondWith(
      caches.match(request).then(
        (cached) =>
          cached ||
          fetch(request).then((response) => {
            const copy = response.clone()
            caches.open(CACHE).then((cache) => cache.put(request, copy))
            return response
          }),
      ),
    )
  }
})
