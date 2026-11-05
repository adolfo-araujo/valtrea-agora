import { test, expect } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'
import { execSync } from 'node:child_process'

// Confirma o e-mail direto no banco do CI (o código vai por e-mail, que no CI só aparece no log da API).
const sql = (comando) => execSync(`psql "${process.env.DATABASE_URL_PSQL}" -c "${comando}"`)
const confirmarNoBanco = (email) => sql(`UPDATE usuarios SET email_confirmado_em = now() WHERE email = '${email}'`)

async function semProblemasDeAcessibilidade(page, nome) {
  const { violations } = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze()
  const lista = violations.map((v) => `- ${v.id}: ${v.help} (${v.nodes.length}x) ${v.nodes[0]?.target.join(' ')}`).join('\n')
  expect(violations, `Problemas de acessibilidade em ${nome}:\n${lista}`).toEqual([])
}

// Todos os testes falham se a CSP de produção bloquear algo ou se a página tiver erro de JavaScript.
test.beforeEach(async ({ page }, testInfo) => {
  testInfo.problemas = []
  page.on('console', (msg) => {
    if (msg.type() === 'error' && /Content Security Policy|Refused to/i.test(msg.text())) testInfo.problemas.push(msg.text())
  })
  page.on('pageerror', (e) => testInfo.problemas.push(e.message))
})
test.afterEach(async ({}, testInfo) => {
  expect(testInfo.problemas, testInfo.problemas.join('\n')).toEqual([])
})

for (const [nome, rota] of [
  ['início', '/'],
  ['entrar', '/entrar'],
  ['criar conta', '/cadastro'],
  ['esqueci a senha', '/esqueci-senha'],
  ['termos', '/termos'],
  ['privacidade', '/privacidade'],
  ['acessibilidade', '/acessibilidade'],
  ['excluir conta', '/excluir-conta'],
  ['estabelecimento inexistente', '/e/00000000-0000-0000-0000-000000000000'],
]) {
  test(`acessibilidade: ${nome}`, async ({ page }) => {
    await page.goto(rota)
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h1')).toBeVisible()
    await semProblemasDeAcessibilidade(page, nome)
  })
}

async function criarConta(page, nome, email) {
  await page.goto('/cadastro')
  await page.getByLabel('Nome').fill(nome)
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('Senha').fill('senha-forte-123')
  await page.getByLabel(/Tenho 18 anos/).check()
  await page.getByRole('button', { name: 'Criar conta' }).click()
  await expect(page.getByRole('heading', { name: 'Confirme seu e-mail' })).toBeVisible()
  confirmarNoBanco(email)
}

test('fluxo completo: o negócio é aprovado, publica um horário, o cliente reserva e os dois veem', async ({ page, browser }, testInfo) => {
  const sufixo = `${testInfo.project.name}-${Date.now()}`
  const cidade = `Cidade Ensaio ${sufixo}`
  const nome = `Barbearia Ensaio ${sufixo}`
  const amanha = new Date(Date.now() + 24 * 3600 * 1000)
  const dia = `${amanha.getFullYear()}-${String(amanha.getMonth() + 1).padStart(2, '0')}-${String(amanha.getDate()).padStart(2, '0')}`

  // O dono cria a conta e cadastra o estabelecimento
  await criarConta(page, 'Beto Dono', `beto-${sufixo}@test.dev`)
  await page.goto('/negocio')
  await page.getByRole('button', { name: 'Cadastrar estabelecimento' }).click()
  await page.getByLabel('Nome do estabelecimento').fill(nome)
  await page.getByLabel('Tipo', { exact: true }).selectOption('BARBEARIA')
  await page.getByLabel('Endereço (rua e número)').fill('Rua das Flores, 100')
  await page.getByLabel('Bairro', { exact: true }).fill('Centro')
  await page.getByLabel('Cidade', { exact: true }).fill(cidade)
  await page.getByLabel('Estado', { exact: true }).selectOption('RJ')
  await page.getByLabel('Telefone do estabelecimento').fill('(21) 99999-0000')
  await semProblemasDeAcessibilidade(page, 'cadastro do estabelecimento')
  await page.getByRole('button', { name: 'Enviar para análise' }).click()
  await expect(page.getByRole('heading', { name: nome })).toBeVisible()
  await expect(page.getByText('Em análise', { exact: true })).toBeVisible()

  // Publica um horário para amanhã
  await page.getByLabel('Dia', { exact: true }).fill(dia)
  await page.getByLabel('Horários vagos', { exact: true }).fill('15:00')
  await page.getByLabel('Serviço', { exact: true }).fill('Corte e barba')
  await page.getByLabel('Preço (opcional)').fill('45,00')
  await page.getByRole('button', { name: 'Publicar horário' }).click()
  await expect(page.getByText('Horários publicados.')).toBeVisible()
  await semProblemasDeAcessibilidade(page, 'agenda do dono')
  const endereco = new URL(page.url()).pathname
  const id = endereco.split('/').pop()

  // Em análise, não aparece para ninguém; a administração aprova (aqui, direto no banco)
  const clienteCtx = await browser.newContext()
  const cliente = await clienteCtx.newPage()
  await cliente.goto(`/e/${id}`)
  await expect(cliente.getByRole('heading', { name: 'Estabelecimento não encontrado' })).toBeVisible()
  sql(`UPDATE estabelecimentos SET status = 'APROVADO' WHERE id = '${id}'`)

  // O cliente procura, acha e reserva
  await criarConta(cliente, 'Carla Cliente', `carla-${sufixo}@test.dev`)
  await cliente.goto('/')
  await cliente.getByLabel('Estado', { exact: true }).selectOption('RJ')
  await cliente.getByLabel('Cidade ou bairro').fill(cidade)
  await cliente.getByRole('button', { name: 'Procurar' }).click()
  await expect(cliente.getByRole('heading', { name: 'Amanhã' })).toBeVisible()
  await semProblemasDeAcessibilidade(cliente, 'busca de horários')
  await cliente.getByRole('link', { name: new RegExp(nome) }).click()
  await expect(cliente.getByRole('heading', { name: nome })).toBeVisible()
  await semProblemasDeAcessibilidade(cliente, 'página do estabelecimento')
  await cliente.getByRole('button', { name: /Reservar Corte e barba/ }).click()
  await cliente.getByLabel('Seu telefone (opcional)').fill('(21) 98888-7777')
  await cliente.getByRole('button', { name: 'Confirmar reserva' }).click()
  await expect(cliente.getByText('Reserva feita!')).toBeVisible()
  await expect(cliente.getByRole('link', { name: nome })).toBeVisible()
  await semProblemasDeAcessibilidade(cliente, 'minhas reservas')
  await clienteCtx.close()

  // O dono vê a reserva com o primeiro nome e o telefone
  await page.goto(endereco)
  await expect(page.getByText('Carla', { exact: true })).toBeVisible()
  await expect(page.getByText('(21) 98888-7777')).toBeVisible()
  await expect(page.getByText('Carla Cliente')).toHaveCount(0)
})
