-- =====================================================================
-- V2: dados FICTÍCIOS de demonstração
-- =====================================================================

INSERT INTO material (nome, descricao, unidade_medida, orientacao_preparo, ativo) VALUES
('Papel',    'Papel e papelão limpos e secos',        'KG',      'Manter seco; desmontar caixas de papelão', TRUE),
('Plástico', 'Garrafas PET e embalagens plásticas',   'KG',      'Lavar e retirar as tampas',                TRUE),
('Vidro',    'Garrafas e potes de vidro',             'KG',      'Lavar; embalar cacos para evitar cortes',  TRUE),
('Metal',    'Latas de alumínio e aço',               'KG',      'Lavar e amassar as latas',                 TRUE),
('Pilha',    'Pilhas e baterias pequenas',            'UNIDADE', 'Entregar embaladas em ponto específico',   FALSE);

INSERT INTO ponto_coleta (nome, logradouro, numero, bairro, municipio, uf, cep, contato, horarios, ativo, recolhimento_domiciliar) VALUES
('Cooperativa EcoVida',  'Rua das Flores',  '120', 'Centro', 'Aracaju', 'SE', '49010000', '(79) 3000-0000', 'Seg–Sex 8h–17h', TRUE, TRUE),
('Ecoponto Bairro Sul',  'Avenida Principal', '45', 'Sul',   'Aracaju', 'SE', '49020000', '(79) 3000-0001', 'Sáb 8h–12h',     TRUE, FALSE),
('Recicla Lagarto',      'Rua do Comércio', '8',   'Centro', 'Lagarto', 'SE', '49400000', '(79) 3000-0002', 'Seg–Sex 9h–16h', TRUE, TRUE);

INSERT INTO material_aceito (ponto_id, material_id, observacoes, limite_por_solicitacao)
SELECT p.id, m.id, v.obs, v.lim
FROM (VALUES
    ('Cooperativa EcoVida', 'Papel',    'Papelão desmontado', 50.000),
    ('Cooperativa EcoVida', 'Plástico', NULL,                 30.000),
    ('Cooperativa EcoVida', 'Vidro',    'Sem cacos soltos',   20.000),
    ('Ecoponto Bairro Sul', 'Vidro',    NULL,                 NULL),
    ('Ecoponto Bairro Sul', 'Metal',    NULL,                 NULL),
    ('Recicla Lagarto',     'Papel',    NULL,                 40.000),
    ('Recicla Lagarto',     'Vidro',    NULL,                 25.000),
    ('Recicla Lagarto',     'Metal',    NULL,                 25.000)
) AS v(ponto, material, obs, lim)
JOIN ponto_coleta p ON p.nome = v.ponto
JOIN material m     ON m.nome = v.material;

INSERT INTO operador (nome, email, ativo) VALUES
('Maria Souza',   'maria.operadora@exemplo.com', TRUE),
('João Pereira',  'joao.operador@exemplo.com',   TRUE);

INSERT INTO operador_ponto (operador_id, ponto_id)
SELECT o.id, p.id
FROM (VALUES
    ('maria.operadora@exemplo.com', 'Cooperativa EcoVida'),
    ('maria.operadora@exemplo.com', 'Ecoponto Bairro Sul'),
    ('joao.operador@exemplo.com',   'Recicla Lagarto')
) AS v(email, ponto)
JOIN operador o      ON o.email = v.email
JOIN ponto_coleta p  ON p.nome = v.ponto;

INSERT INTO morador (nome, email, telefone, ativo) VALUES
('Ana Souza',    'ana.souza@exemplo.com',    '(79) 90000-0001', TRUE),
('Carlos Lima',  'carlos.lima@exemplo.com',  '(79) 90000-0002', TRUE);

INSERT INTO endereco (morador_id, logradouro, numero, sem_numero, bairro, municipio, uf, cep, complemento)
SELECT mo.id, v.logradouro, v.numero, v.sem_numero, v.bairro, v.municipio, v.uf, v.cep, v.complemento
FROM (VALUES
    ('ana.souza@exemplo.com',   'Rua das Flores',    '120',        FALSE, 'Centro', 'Aracaju', 'SE', '49010000', NULL),
    ('ana.souza@exemplo.com',   'Avenida Principal', CAST(NULL AS VARCHAR), TRUE, 'Sul', 'Aracaju', 'SE', '49020000', 'Casa dos fundos'),
    ('carlos.lima@exemplo.com', 'Rua do Comércio',   '35',         FALSE, 'Centro', 'Lagarto', 'SE', '49400000', 'Apto 2')
) AS v(email, logradouro, numero, sem_numero, bairro, municipio, uf, cep, complemento)
JOIN morador mo ON mo.email = v.email;
