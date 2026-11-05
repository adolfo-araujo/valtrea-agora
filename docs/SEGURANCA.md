# Segurança

Os mesmos 20 itens revisados no Valtrea Finance, no Pay, no Volta e no Pulso. O CI confere boa parte deles a cada mudança (testes da API com PostgreSQL real, testes no navegador, gitleaks e OSV-Scanner).

| # | Item | Como está |
| --- | --- | --- |
| 1 | Chaves de API escondidas | O app e o site só têm o endereço da API; segredos ficam no Render |
| 2 | Segredos fora do Git | `.gitignore` + gitleaks em todo o histórico |
| 3 | Chave pública do banco | Não se aplica: só a API fala com o banco |
| 4 | Acesso por linha | O id de quem pede vem sempre do token; agenda, horários e estabelecimento de outra pessoa respondem 404; só o cliente ou o dono cancelam uma reserva; estabelecimento em análise responde 404 para todos, menos o dono (testado) |
| 5 | Criptografia | HTTPS; banco cifrado em repouso; bcrypt; sessões SHA-256; códigos de e-mail HMAC; backup AES-256 |
| 6 | Autenticação no servidor | JWT de 15 min em toda rota não pública; sessão de 30 dias com token de uso único |
| 7 | Restringir acessos | O estabelecimento vê só o primeiro nome do cliente e o que ele quis informar; ninguém vê quem denunciou; análise só para administradores |
| 8 | Mass assignment | Pedidos são `record`s com só os campos permitidos |
| 9 | Cookies | Não usa: o login vai no cabeçalho Authorization |
| 10 | Hash de senha | bcrypt; 8 a 72 caracteres com letras e números |
| 11 | Limite de pedidos | 300/5 min por IP; 30 logins e 20 cadastros por IP a cada 15 min; 120 reservas, cancelamentos, horários e denúncias por IP por hora; até 3 reservas futuras por pessoa; bloqueio após 5 senhas erradas |
| 12 | Robôs e abuso | E-mail confirmado para reservar, cadastrar e denunciar; estabelecimento só aparece depois da análise; 3 denúncias o tiram da busca; uma reserva por horário mesmo com cliques simultâneos (trava + índice único, testado) |
| 13 | Consultas parametrizadas | JPA e SQL fixo com parâmetros |
| 14 | Validação | Bean Validation em todos os pedidos; corpo até 100 KB; textos com tamanho mínimo e máximo; UF só entre as 27; horários de 10 min a 60 dias à frente |
| 15 | Vazamento de conteúdo | Erros sem detalhes; logs e Sentry com a rota modelo (sem ids) |
| 16 | Uploads | Não se aplica |
| 17 | Respostas enxutas | Só o que o app e o site usam |
| 18 | Cabeçalhos | API: CSP `default-src 'none'`, nosniff, X-Frame-Options, HSTS, Referrer-Policy, Permissions-Policy, COOP. Site: CSP só com o próprio endereço e a API, `Referrer-Policy: no-referrer` |
| 19 | Forçar HTTPS | Render e Vercel redirecionam; o app de produção bloqueia HTTP |
| 20 | Dependências | OSV-Scanner (reprova falha crítica) e Dependabot para Maven, Gradle, npm, Docker e Actions |

CORS: a API só aceita chamadas de navegador vindas de `VALTREA_CORS` (o site do Agora).
