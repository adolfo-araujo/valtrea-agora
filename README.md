# Valtrea Agora

Horários vagos de salões, barbearias e outros serviços da região, para reservar ainda hoje. Estabelecimentos publicam os horários; clientes reservam sem pagar nada no app (o pagamento é no local).

| Parte | Pasta | Tecnologia | Onde roda |
| --- | --- | --- | --- |
| API | `api/` | Java 21, Spring Boot 4.1, PostgreSQL, Flyway | Render (`valtrea-agora-api.onrender.com`) |
| Site | `web/` | React, Vite, Tailwind | Vercel (`agora.valtrea.com.br`) |
| App | `android/` | Kotlin, Jetpack Compose | Google Play |

Qualquer pessoa vê os horários e os estabelecimentos aprovados no site, sem conta. Para reservar, cadastrar um estabelecimento e denunciar, é preciso conta com o e-mail confirmado. A mesma conta pode ser cliente e dona de estabelecimento.

## Como funciona

1. O dono cadastra o estabelecimento (nome, tipo, endereço, bairro, cidade, telefone). Ele fica **em análise** e só aparece depois que a administração aprova no painel. Se for recusado, o dono recebe o motivo por e-mail; ao corrigir, volta para a fila.
2. O dono publica horários vagos (até 20 de uma vez, de daqui a 10 minutos até 60 dias), com serviço, duração e preço (só informativo).
3. O cliente procura por estado, cidade ou bairro (sem acento) e tipo de serviço, vê os horários dos próximos 7 dias e reserva. Até 3 reservas futuras por pessoa. Dois cliques no mesmo horário ao mesmo tempo: só um reserva (trava da linha + índice único parcial).
4. O dono recebe um e-mail e vê na agenda o primeiro nome do cliente e, se ele informou, o telefone e a observação. Depois do horário, marca "atendido" ou "não veio".
5. Cancelamento: o cliente cancela antes do horário (o horário volta a ficar livre) ou o dono cancela (não volta). O outro lado recebe um e-mail.
6. Com 3 denúncias de clientes diferentes, o estabelecimento sai da busca e volta para a fila de análise.

## Segurança e privacidade

- Sem pagamento pelo app: não há cartão, Pix nem dados bancários. Os Termos proíbem pedir pagamento antecipado e a página avisa o cliente.
- O estabelecimento vê só o primeiro nome do cliente; nunca o e-mail nem as outras reservas. A administração vê o primeiro nome do dono e os motivos das denúncias, nunca quem denunciou.
- Estabelecimento em análise ou recusado responde 404 para todos, menos para o dono.
- Limite por IP para reservar, cancelar, publicar horários e denunciar (120 por hora).
- Excluir a conta apaga na hora as reservas, os estabelecimentos (com horários e reservas) e as denúncias.
- Retenção: horários e reservas 90 dias depois do horário; recusados não corrigidos 30 dias; cadastros nunca confirmados 30 dias.
- Os mesmos controles do Valtrea Pay, do Volta e do Pulso: bcrypt, sessões de uso único, bloqueio de login, limites por IP, cabeçalhos de segurança, CORS só para o site, Sentry sem dados pessoais, backup diário criptografado, OSV-Scanner e gitleaks no CI.

## Rotas da API

Todas em `/api/v1`. Documentação interativa em `/swagger-ui.html`.

| Método | Rota | O que faz |
| --- | --- | --- |
| POST | `/auth/cadastro`, `/auth/login`, `/auth/renovar`, `/auth/sair` | Conta e sessão |
| POST | `/auth/confirmar-email`, `/auth/reenviar-confirmacao`, `/auth/esqueci-senha`, `/auth/redefinir-senha` | Códigos por e-mail |
| GET, PATCH, DELETE | `/eu` | Perfil, troca do nome e exclusão da conta |
| GET | `/eu/dados` | Baixar meus dados (JSON) |
| POST | `/eu/termos` | Aceite da versão vigente dos Termos |
| GET | `/publico/horarios`, `/publico/estabelecimentos`, `/publico/estabelecimentos/{id}` | Busca sem conta |
| GET | `/estabelecimentos/{id}` | Detalhe, dizendo se é seu ou se você denunciou |
| POST | `/horarios/{id}/reserva` | Reservar |
| GET | `/eu/reservas` | As suas reservas |
| POST | `/reservas/{id}/cancelar`, `/reservas/{id}/status` | Cancelar (cliente ou dono) e marcar presença (dono) |
| GET, POST | `/eu/estabelecimentos`, `/estabelecimentos` | Os seus estabelecimentos e cadastrar (até 3) |
| PUT, DELETE | `/estabelecimentos/{id}` | Editar e apagar |
| GET, POST | `/estabelecimentos/{id}/agenda`, `/estabelecimentos/{id}/horarios` | Agenda do dono e publicar horários |
| DELETE | `/horarios/{id}` | Apagar horário sem reserva |
| POST | `/estabelecimentos/{id}/denuncia` | Denunciar |
| GET, POST | `/admin/estabelecimentos`, `/admin/estabelecimentos/{id}/aprovar`, `/admin/estabelecimentos/{id}/recusar` | Fila de análise (só administradores) |
| GET | `/admin/metricas` | Números de uso somados (só administradores) |

## Rodar no computador

```bash
# Banco
docker run -d --name agora-db -p 5432:5432 -e POSTGRES_USER=valtrea -e POSTGRES_PASSWORD=valtrea -e POSTGRES_DB=valtrea_agora postgres:18-alpine
# API (porta 8080)
cd api
VALTREA_JWT_SECRET=$(openssl rand -hex 32) \
VALTREA_URL_SITE=http://localhost:5175 VALTREA_CORS=http://localhost:5175 mvn spring-boot:run
# Site (porta 5175)
cd ../web && npm install && npm run dev
```

Sem SMTP configurado, os e-mails (códigos e avisos) aparecem no log da API. O app Android (variante `debug`) usa `http://10.0.2.2:8080/`.

## Publicar

1. **Banco (Neon):** crie o projeto `valtrea-agora` na região **AWS US East 1 (N. Virginia)**, a mesma do Render. O teste de capacidade do Pay mostrou que o banco em São Paulo com a API nos EUA deixa cada ida ao banco com ~120 ms.
2. **API (Render):** New → Blueprint → repositório `valtrea-agora`. Preencha `DATABASE_URL` (`jdbc:postgresql://<host>/<banco>?sslmode=require`), `DATABASE_USER`, `DATABASE_PASSWORD` e `RESEND_API_KEY`.
3. **Site (Vercel):** Add New → Project → repositório `valtrea-agora`, **Root Directory** `web`, framework Vite. Em Environment Variables, `VITE_API_URL` = `https://valtrea-agora-api.onrender.com`. Em Domains, adicione `agora.valtrea.com.br`.
4. **DNS:** crie o registro CNAME que a Vercel mostrar para `agora`.
5. **App:** a variante `release` já aponta para `https://valtrea-agora-api.onrender.com/`.

