-- Valtrea Agora: clientes, estabelecimentos (salões, barbearias...), horários vagos e reservas.
-- Excluir uma conta apaga tudo dela em cascata (LGPD: exclusão na hora).

CREATE TABLE usuarios (
    id                        UUID PRIMARY KEY,
    nome                      VARCHAR(120) NOT NULL,
    email                     VARCHAR(254) NOT NULL,
    senha_hash                VARCHAR(100) NOT NULL,
    papel                     VARCHAR(20)  NOT NULL,
    tentativas_login_falhas   INTEGER      NOT NULL DEFAULT 0,
    bloqueado_ate             TIMESTAMPTZ,
    email_confirmado_em       TIMESTAMPTZ,
    criado_em                 TIMESTAMPTZ  NOT NULL,
    termos_versao             VARCHAR(20),
    termos_aceitos_em         TIMESTAMPTZ,
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

-- Sessões do app e do site. Só o hash do token: quem ler o banco não consegue usá-lo.
CREATE TABLE sessoes (
    id              UUID PRIMARY KEY,
    usuario_id      UUID        NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    token_hash      VARCHAR(64) NOT NULL,
    expira_em       TIMESTAMPTZ NOT NULL,
    criado_em       TIMESTAMPTZ NOT NULL,
    revogado_em     TIMESTAMPTZ,
    CONSTRAINT uk_sessoes_token UNIQUE (token_hash)
);
CREATE INDEX ix_sessoes_usuario ON sessoes (usuario_id);

-- Códigos de 6 dígitos enviados por e-mail (confirmação de e-mail e nova senha), só como HMAC.
CREATE TABLE codigos_email (
    id              UUID PRIMARY KEY,
    usuario_id      UUID        NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    finalidade      VARCHAR(20) NOT NULL,
    codigo_hash     VARCHAR(64) NOT NULL,
    tentativas      INTEGER     NOT NULL DEFAULT 0,
    expira_em       TIMESTAMPTZ NOT NULL,
    criado_em       TIMESTAMPTZ NOT NULL,
    usado_em        TIMESTAMPTZ
);
CREATE INDEX ix_codigos_email_usuario ON codigos_email (usuario_id, finalidade, criado_em DESC);

-- Estabelecimento: só aparece ao público depois de aprovado pela administração.
CREATE TABLE estabelecimentos (
    id              UUID PRIMARY KEY,
    dono_id         UUID         NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    nome            VARCHAR(80)  NOT NULL,
    tipo            VARCHAR(30)  NOT NULL,
    descricao       VARCHAR(500),
    endereco        VARCHAR(160) NOT NULL,
    bairro          VARCHAR(80)  NOT NULL,
    cidade          VARCHAR(80)  NOT NULL,
    uf              VARCHAR(2)   NOT NULL,
    telefone        VARCHAR(20)  NOT NULL,
    -- cidade e bairro sem acentos e em minúsculas, para a busca
    busca           VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    motivo_recusa   VARCHAR(300),
    revisado_em     TIMESTAMPTZ,
    criado_em       TIMESTAMPTZ  NOT NULL,
    atualizado_em   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_estabelecimentos_dono ON estabelecimentos (dono_id);
CREATE INDEX ix_estabelecimentos_busca ON estabelecimentos (status, uf, busca);

-- Horário vago publicado pelo estabelecimento. O preço é só informativo (o pagamento é no local).
CREATE TABLE horarios (
    id                  UUID PRIMARY KEY,
    estabelecimento_id  UUID        NOT NULL REFERENCES estabelecimentos (id) ON DELETE CASCADE,
    inicio              TIMESTAMPTZ NOT NULL,
    duracao_min         INTEGER     NOT NULL,
    servico             VARCHAR(80) NOT NULL,
    preco_centavos      INTEGER,
    status              VARCHAR(20) NOT NULL,
    criado_em           TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_horarios_estabelecimento ON horarios (estabelecimento_id, inicio);
CREATE INDEX ix_horarios_livres ON horarios (status, inicio);

-- Reserva de um horário. Só uma confirmada por horário (índice parcial), mesmo com dois cliques ao mesmo tempo.
CREATE TABLE reservas (
    id              UUID PRIMARY KEY,
    horario_id      UUID         NOT NULL REFERENCES horarios (id) ON DELETE CASCADE,
    cliente_id      UUID         NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    telefone        VARCHAR(20),
    observacao      VARCHAR(200),
    status          VARCHAR(30)  NOT NULL,
    criado_em       TIMESTAMPTZ  NOT NULL,
    atualizado_em   TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX uk_reservas_horario_confirmada ON reservas (horario_id) WHERE status = 'CONFIRMADA';
CREATE INDEX ix_reservas_cliente ON reservas (cliente_id, criado_em DESC);

-- Uma denúncia por pessoa em cada estabelecimento. Apagadas quando a administração decide.
CREATE TABLE denuncias (
    estabelecimento_id  UUID        NOT NULL REFERENCES estabelecimentos (id) ON DELETE CASCADE,
    usuario_id          UUID        NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    motivo              VARCHAR(30) NOT NULL,
    criado_em           TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (estabelecimento_id, usuario_id)
);
