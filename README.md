# Sistema de Gerenciamento de Loja de Computadores

> Trabalho acadêmico — sistema desktop de controle de produtos, estoque e vendas de uma loja de informática.

Sistema desktop em **Java 17 + Swing + MySQL (JDBC)**, desenvolvido conforme o *Documento de Esboço do Projeto v1.0*.

## 1. O que você precisa instalar
| Item | Versão |
|---|---|
| JDK | 17 ou superior |
| MySQL Server | **8.0.16 ou superior** (usa `CHECK` e `SELECT … FOR UPDATE OF`) |
| IntelliJ IDEA | Community ou Ultimate |

## 2. Configurar o banco de dados
1. Abra o MySQL (Workbench, DBeaver ou terminal) e execute o arquivo **`sql/schema.sql`**.
   Ele cria o banco `loja_informatica`, todas as tabelas, chaves, restrições e as categorias iniciais.
2. Em `src/main/resources/`, **copie** `db.properties.example` para um arquivo novo chamado **`db.properties`** e coloque nele o seu usuário e a sua senha do MySQL.
   (O `db.properties` está no `.gitignore`: a sua senha **não** vai para o GitHub.)

## 3. Executar no IntelliJ IDEA
1. `File > Open…` e selecione a pasta do projeto (onde está o `pom.xml`). Aceite "Trust project" / "Load Maven project".
2. Aguarde o Maven baixar o driver `mysql-connector-j` (único requisito externo — precisa de internet na primeira vez).
3. `File > Project Structure > Project` → SDK 17+.
4. Abra `src/main/java/loja/Main.java` e clique no ▶ ao lado de `main`.
5. **Primeira execução:** como ainda não existe usuário, o programa abre uma janela para criar o **administrador inicial**
   (seção 5.2 do documento). Depois, entre pelo login e cadastre os vendedores em *Usuários*.

> Para gerar um `.jar` executável: `mvn package` → `java -jar target/loja-computadores-1.0.jar`.

## 4. Organização do código (camadas da seção 14)
```
src/main/java/loja/
├── Main.java            Ponto de entrada
├── model/               ENTIDADES: Usuario, Produto, Categoria, Venda, ItemVenda, Movimentacao + enums
├── dao/                 ACESSO A DADOS: todo o SQL (sempre parametrizado — RN14)
├── service/             REGRAS DE NEGÓCIO e permissões
│   ├── Sessao           usuário logado + exigirAdmin()/exigirLogado()
│   ├── AuthService      login, admin inicial
│   ├── UsuarioService / CategoriaService / ProdutoService
│   ├── EstoqueService   entradas e histórico de movimentações
│   ├── Carrinho         venda em montagem (cálculo de subtotal/total, anti-duplicidade)
│   ├── VendaService     confirmar e cancelar vendas (transações)
│   └── RelatorioService painel e relatórios
├── ui/                  INTERFACE Swing (sem SQL e sem regra de negócio)
└── util/                Database (conexão/transação), SenhaUtil, Fmt, Validar, exceções
```
**Fluxo:** tela (`ui`) → serviço (`service`, valida permissão e regras) → DAO (`dao`, SQL) → MySQL.

## 5. Como as regras importantes foram implementadas
| Regra | Onde / como |
|---|---|
| RN02/RN18 permissões | `Sessao.exigirAdmin()` no início de cada operação administrativa dos *services* (não só esconder botões). Vendedor só vê as próprias vendas — filtro imposto em `VendaService`. |
| RN05/RN10 estoque | `Carrinho.adicionar` valida na tela; `VendaService.confirmar` **revalida** no banco com `SELECT … FOR UPDATE` (trava a linha: duas vendas simultâneas não usam o mesmo estoque). |
| RN07/RN13 transação | `Database.transacao(...)`: venda + itens + baixa de estoque + movimentações em um único *commit*; qualquer erro faz *rollback*. |
| RN08 preço histórico | `itens_venda.preco_unitario` guarda o preço do momento. |
| RN11 duplicidade | botão desabilitado + `Carrinho` (flag em processamento/finalizada) + coluna `vendas.token UNIQUE`. |
| RN15 dinheiro | `BigDecimal` no Java e `DECIMAL(12,2)` no MySQL. |
| RN16 exclusão | Produto com venda/movimentação é apenas **desativado**; sem histórico é excluído de fato. |
| Cancelamento | Marca `CANCELADA`, devolve estoque, grava movimentações `ENTRADA_CANCELAMENTO`; segunda tentativa é bloqueada. Sem estorno financeiro (como no documento). |
| Senhas | PBKDF2-HMAC-SHA256 + salt aleatório (`SenhaUtil`); nunca em texto puro. |

## 6. Decisões e pequenas interpretações (seção 21, item 11 — nada foi alterado em silêncio)
1. **Swing** foi escolhido (já vem no JDK, sem dependências).
2. **Administrador também pode vender** (a seção 4.1 diz que ele tem acesso às funcionalidades "operacionais").
3. Adicionei uma tela de **Categorias**, pois "Cadastro de categorias" é obrigatório na seção 18, mas não aparece na lista de telas.
4. **Estoque inicial** informado no cadastro do produto gera uma movimentação de entrada ("Estoque inicial"), para o histórico bater com o saldo.
5. Se o **preço mudar** entre adicionar o item e confirmar a venda, o sistema pede para refazer o item (evita cobrar valor diferente do que o vendedor viu).
6. Proteções extras: não é possível desativar a própria conta nem o **último administrador ativo**.
7. Formato de datas nos filtros: `dd/mm/aaaa`.

## 7. Roteiro de testes (seção 20)
1. Crie o admin, cadastre um produto (código `RAM16`, R$ 200,00, estoque 10) → deve aparecer na lista; tente repetir o código → deve recusar.
2. Cadastre um vendedor, saia e entre como vendedor: o menu não mostra Estoque/Usuários/Relatórios.
3. Venda 3 un. → estoque cai para 7; tente vender 8 → bloqueado.
4. Altere o preço do produto → a venda antiga continua com o preço original.
5. Como admin, cancele a venda → estoque volta, venda fica "Cancelada"; tente cancelar de novo → bloqueado.
6. Veja *Estoque > Histórico de movimentações* e *Relatórios*.
7. Feche e abra o programa: tudo continua salvo.
