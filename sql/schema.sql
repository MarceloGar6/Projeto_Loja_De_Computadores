-- =====================================================================
--  Sistema de Gerenciamento de Loja de Computadores — estrutura do banco
--  Execute este script UMA vez no MySQL (Workbench, DBeaver ou terminal).
-- =====================================================================
CREATE DATABASE IF NOT EXISTS loja_informatica
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE loja_informatica;

CREATE TABLE IF NOT EXISTS usuarios (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    nome_completo VARCHAR(120) NOT NULL,
    nome_usuario  VARCHAR(50)  NOT NULL,
    email         VARCHAR(120) NOT NULL,
    senha_hash    VARCHAR(255) NOT NULL,          -- PBKDF2 + salt (nunca texto puro)
    tipo          ENUM('ADMIN','VENDEDOR') NOT NULL,
    ativo         BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuarios_nome_usuario UNIQUE (nome_usuario),
    CONSTRAINT uq_usuarios_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categorias (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nome      VARCHAR(80) NOT NULL,
    descricao VARCHAR(255),
    ativo     BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_categorias_nome UNIQUE (nome)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS produtos (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    codigo         VARCHAR(40)  NOT NULL,
    nome           VARCHAR(150) NOT NULL,
    descricao      VARCHAR(500),
    marca          VARCHAR(80),
    modelo         VARCHAR(80),
    categoria_id   INT NOT NULL,
    preco_venda    DECIMAL(12,2) NOT NULL,         -- DECIMAL: nada de float/double (RN15)
    quantidade     INT NOT NULL DEFAULT 0,
    estoque_minimo INT NOT NULL DEFAULT 0,
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_produtos_codigo UNIQUE (codigo),
    CONSTRAINT fk_produtos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    CONSTRAINT ck_produtos_preco CHECK (preco_venda >= 0),
    CONSTRAINT ck_produtos_qtd CHECK (quantidade >= 0),
    CONSTRAINT ck_produtos_min CHECK (estoque_minimo >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vendas (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    vendedor_id     INT NOT NULL,
    data_hora       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valor_total     DECIMAL(12,2) NOT NULL,
    forma_pagamento ENUM('DINHEIRO','DEBITO','CREDITO','PIX') NOT NULL,
    situacao        ENUM('CONCLUIDA','CANCELADA') NOT NULL DEFAULT 'CONCLUIDA',
    token           VARCHAR(36) NOT NULL,           -- impede venda duplicada por clique repetido (RN11)
    cancelada_por   INT NULL,
    cancelada_em    DATETIME NULL,
    INDEX idx_vendas_data (data_hora),
    CONSTRAINT uq_vendas_token UNIQUE (token),
    CONSTRAINT fk_vendas_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuarios(id),
    CONSTRAINT fk_vendas_cancelada_por FOREIGN KEY (cancelada_por) REFERENCES usuarios(id),
    CONSTRAINT ck_vendas_total CHECK (valor_total >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS itens_venda (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    venda_id       INT NOT NULL,
    produto_id     INT NOT NULL,
    quantidade     INT NOT NULL,
    preco_unitario DECIMAL(12,2) NOT NULL,          -- preço no momento da venda (RN08)
    subtotal       DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_itens_venda FOREIGN KEY (venda_id) REFERENCES vendas(id),
    CONSTRAINT fk_itens_produto FOREIGN KEY (produto_id) REFERENCES produtos(id),
    CONSTRAINT ck_itens_qtd CHECK (quantidade > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS movimentacoes_estoque (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    produto_id  INT NOT NULL,
    usuario_id  INT NOT NULL,
    venda_id    INT NULL,
    tipo        ENUM('ENTRADA','SAIDA_VENDA','ENTRADA_CANCELAMENTO') NOT NULL,
    quantidade  INT NOT NULL,
    data_hora   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacao  VARCHAR(255),
    CONSTRAINT fk_mov_produto FOREIGN KEY (produto_id) REFERENCES produtos(id),
    CONSTRAINT fk_mov_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_mov_venda FOREIGN KEY (venda_id) REFERENCES vendas(id),
    CONSTRAINT ck_mov_qtd CHECK (quantidade > 0)
) ENGINE=InnoDB;

-- Categorias iniciais sugeridas no documento do projeto
INSERT IGNORE INTO categorias (nome, descricao) VALUES
 ('Computadores','Desktops, notebooks e all-in-one'),
 ('Processadores','CPUs'),
 ('Placas-mãe','Motherboards'),
 ('Memórias RAM','Módulos de memória'),
 ('Armazenamento','SSDs, HDs e pen drives'),
 ('Placas de vídeo','GPUs'),
 ('Periféricos','Teclados, mouses, headsets'),
 ('Monitores','Monitores e telas'),
 ('Acessórios','Cabos, adaptadores e outros');

-- OBS: o administrador inicial NÃO é criado aqui (a senha precisa de hash seguro).
-- Na primeira execução do programa, uma tela pede os dados do administrador (seção 5.2 do documento).
