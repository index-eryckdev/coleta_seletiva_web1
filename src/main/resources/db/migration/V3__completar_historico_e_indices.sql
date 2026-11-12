ALTER TABLE ponto_coleta ADD COLUMN complemento VARCHAR(80);
ALTER TABLE ponto_coleta ALTER COLUMN numero DROP NOT NULL;
ALTER TABLE solicitacao ADD COLUMN data_atualizacao TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE solicitacao ADD COLUMN tentativa INTEGER NOT NULL DEFAULT 0 CHECK (tentativa >= 0);
ALTER TABLE historico ADD COLUMN data_agendamento TIMESTAMPTZ;

UPDATE solicitacao s
SET data_atualizacao = GREATEST(s.data_criacao, (
        SELECT max(h.data_hora) FROM historico h WHERE h.solicitacao_id = s.id
    )),
    tentativa = (
        SELECT count(*) FROM historico h
        WHERE h.solicitacao_id = s.id AND h.situacao_nova = 'EM_RECOLHIMENTO'
    );

ALTER TABLE item_solicitacao ADD CONSTRAINT ck_item_quantidade_pecas CHECK (
    unidade <> 'UNIDADE' OR
    (quantidade_estimada = trunc(quantidade_estimada) AND
     (quantidade_efetiva IS NULL OR quantidade_efetiva = trunc(quantidade_efetiva)))
);

CREATE INDEX ix_solicitacao_criacao ON solicitacao (data_criacao DESC, id DESC);
CREATE INDEX ix_solicitacao_situacao_criacao ON solicitacao (situacao, data_criacao DESC);
CREATE INDEX ix_solicitacao_conclusao ON solicitacao (data_conclusao) WHERE situacao = 'CONCLUIDA';
CREATE INDEX ix_ponto_publico_localizacao ON ponto_coleta (lower(municipio), lower(bairro)) WHERE ativo;
