# Registro das Operações de Tratamento de Dados Pessoais: Valtrea Agora

Documento interno (LGPD art. 37), no formato simplificado para agente de tratamento de pequeno porte (Resolução CD/ANPD nº 2/2022). Não publicar.

- **Controlador:** João Adolfo Martins Araújo (pessoa física), Rio de Janeiro/RJ
- **Encarregado / canal:** sem encarregado indicado (dispensa de pequeno porte); canal: contato@valtrea.com.br
- **Elaborado em:** 05/11/2026 · **Atualizado em:** 07/11/2026 · **Revisar:** a cada 12 meses ou a cada nova funcionalidade que trate dados
- **Serviço:** Valtrea Agora (site agora.valtrea.com.br, app Android e API valtrea-agora-api.onrender.com), em testes

## Operações

| # | Operação | Titulares | Dados pessoais | Sensíveis? | Finalidade | Base legal | Onde no sistema | Operadores | Transferência internacional | Retenção | Segurança |
|---|----------|-----------|----------------|-----------|-----------|-----------|-----------------|-----------|-----------------------------|----------|-----------|
| 1 | Cadastro e login | Clientes e donos (18+) | Nome, e-mail, senha (hash), aceite dos Termos | Não | Criar e manter a conta | Art. 7º, V | `/api/v1/auth/*` | Render, Neon | Sim (EUA) | Enquanto a conta existir; nunca confirmados sem estabelecimento: 30 dias | bcrypt, JWT curto, sessões só como SHA-256, limites por IP, bloqueio por tentativas |
| 2 | Estabelecimentos | Donos | Dados do negócio (nome, endereço, telefone, descrição) e o vínculo com a conta | Não | Mostrar o estabelecimento aprovado | Art. 7º, V | `/api/v1/estabelecimentos` | Render, Neon, Vercel | Sim (EUA) | Enquanto existir; recusados: 30 dias | Análise antes de publicar; o público não vê nada da conta do dono |
| 3 | Horários | Donos | Data, serviço, duração e preço | Não | Mostrar os horários vagos | Art. 7º, V | `/api/v1/estabelecimentos/{id}/horarios` | Render, Neon | Sim (EUA) | 90 dias depois do horário | — |
| 4 | Reservas | Clientes | Quem reservou, horário, telefone e observação opcionais, presença | Não | Guardar o horário e permitir o contato sobre a reserva | Art. 7º, V | `/api/v1/horarios/{id}/reserva`, `/api/v1/reservas/*` | Render, Neon | Sim (EUA) | 90 dias depois do horário | O estabelecimento vê só o primeiro nome e o que o cliente informou; até 3 reservas futuras por pessoa |
| 5 | Denúncias e análise | Clientes e donos | Quem denunciou, motivo; dados do negócio | Não | Barrar estabelecimentos falsos e golpes | Art. 7º, IX | `/api/v1/estabelecimentos/{id}/denuncia`, `/api/v1/admin/estabelecimentos` | Render, Neon | Sim (EUA) | Até a decisão | A administração vê motivos e contagens, nunca quem denunciou nem o e-mail do dono |
| 6 | E-mails | Clientes e donos | E-mail, primeiro nome, códigos, horário e serviço reservados | Não | Códigos, nova senha, avisos de reserva, cancelamento e análise | Art. 7º, V | `email/*` | Resend | Não (São Paulo) | Códigos: 15 min, apagados 1 dia após vencer | O e-mail ao estabelecimento leva só o primeiro nome do cliente |
| 7 | Prova do aceite | Clientes e donos | Versão e data do aceite | Não | Comprovar o aceite | Art. 7º, VI | `/api/v1/eu/termos`, cadastro | Render, Neon | Sim (EUA) | Enquanto a conta existir | — |
| 8 | Baixar meus dados | Clientes e donos | Tudo da conta | Não | Acesso e portabilidade | Art. 18, II e V | `/api/v1/eu/dados` | Render | Sim (EUA) | Não guarda | Exige login |
| 9 | Proteção contra abuso | Todos | IP, contagem de pedidos | Não | Bloquear força bruta e spam | Art. 7º, IX | `protecao/*` | Render | Sim (EUA) | IP só em memória, até 1 hora | — |
| 10 | Registro de erros | Todos | Dados técnicos, rota modelo (sem ids) | Não | Corrigir falhas | Art. 7º, IX | `monitoramento/*`, `ValtreaApp.kt` | Sentry | Sim (EUA) | Até 90 dias | Sem usuário, IP, corpo, cabeçalhos |
| 11 | Backup | Todos | Cópia do banco | Não | Recuperar dados | Art. 7º, IX | `.github/workflows/backup.yml` | GitHub | Sim (EUA) | 30 dias | AES-256, senha fora do GitHub |
| 12 | Painel de administração | Todos (em conjunto) | Só contagens | Não | Acompanhar o uso | Art. 7º, IX | `/api/v1/admin/metricas` | Render, Neon | Sim (EUA) | Não guarda | Só administradores |
| 13 | Cópia para uso sem internet | Clientes e donos | Perfil (nome, e-mail) e lista de reservas da própria pessoa | Não | Abrir "Minhas reservas" sem internet | Art. 7º, V | `web/public/sw.js` | Nenhum (fica no navegador da pessoa) | Não | Até sair da conta ou a sessão expirar | Só no aparelho da própria pessoa; apagada em `sessao.limpar()` |

## Direitos do titular

- Cliente e dono: corrigir nome, cancelar reservas, editar ou apagar estabelecimentos, baixar os dados e excluir a conta (site ou app). Sem acesso: `/excluir-conta`.
- Resposta em até 15 dias.

## Operadores

| Fornecedor | Serviço | País | Contrato/DPA verificado? |
|-----------|---------|------|--------------------------|
| Render | API | EUA | Pendente |
| Neon | Banco de dados (AWS US East 1) | EUA | Pendente |
| Vercel | Site | EUA e rede global | Pendente |
| Resend | E-mails | Brasil (São Paulo) | Pendente |
| Sentry | Registro de erros | EUA | Pendente |
| GitHub | Backup criptografado | EUA | Pendente |
| ImprovMX / Google (Gmail) | E-mail de contato | EUA | Pendente |

## Pontos de atenção

- Telefone do cliente: opcional e mostrado só ao estabelecimento daquela reserva. Os Termos proíbem usá-lo para outra coisa (ex.: propaganda).
- Golpes: o Agora não tem pagamento; a página e os Termos avisam para nunca pagar adiantado. Denúncia "Pede pagamento antecipado" leva à análise.
- Presença ("não compareceu") é marcada pelo estabelecimento e não é usada para bloquear ninguém nesta versão.

## Revisão de 07/11/2026

- Nova operação 13 (cópia sem internet), já descrita na Política de privacidade (seção 6).
- Cartaz A4 e botão de agendamento para o site do negócio: usam só dados públicos do estabelecimento aprovado (nome, tipo e endereço da página). Nenhum dado novo.
- Busca rápida (Ctrl+K): só navega entre telas; nada é guardado nem enviado.
- Telas de carregamento, de erro e de lista vazia: sem dados novos.
