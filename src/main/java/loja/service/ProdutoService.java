package loja.service;

import loja.dao.MovimentacaoDAO;
import loja.dao.ProdutoDAO;
import loja.model.Produto;
import loja.model.TipoMovimentacao;
import loja.model.Usuario;
import loja.util.*;

import java.math.BigDecimal;
import java.util.List;

public class ProdutoService {
    private final ProdutoDAO dao = new ProdutoDAO();
    private final MovimentacaoDAO movDao = new MovimentacaoDAO();

    /** Qualquer usuário logado pode consultar; vendedores só enxergam produtos ativos. */
    public List<Produto> pesquisar(String busca, Integer categoriaId, String situacao) {
        Usuario u = Sessao.exigirLogado();
        return Database.consultar(c -> dao.pesquisar(c, busca, categoriaId, situacao, !u.isAdmin()));
    }

    /** Produtos que podem entrar numa venda: ativos e com estoque. */
    public List<Produto> pesquisarParaVenda(String busca) {
        Sessao.exigirLogado();
        return Database.consultar(c -> dao.pesquisar(c, busca, null, null, true))
                .stream().filter(p -> p.quantidade() > 0).toList();
    }

    public void cadastrar(Produto p) {
        Usuario admin = Sessao.exigirAdmin();
        Produto v = validar(p);
        try {
            Database.transacao(c -> {
                if (dao.existeCodigo(c, v.codigo(), 0)) throw new NegocioException("Já existe um produto com o código \"" + v.codigo() + "\".");
                int id = dao.inserir(c, v);
                if (v.quantidade() > 0)   // estoque inicial também fica registrado no histórico (RN17)
                    movDao.inserir(c, id, admin.id(), null, TipoMovimentacao.ENTRADA, v.quantidade(), "Estoque inicial no cadastro");
                return null;
            });
        } catch (DaoException e) {
            if (e.isDuplicado()) throw new NegocioException("Já existe um produto com o código \"" + v.codigo() + "\".");
            throw e;
        }
    }

    /** Edita dados cadastrais. A quantidade só muda pelo módulo de estoque. */
    public void editar(Produto p) {
        Sessao.exigirAdmin();
        Produto v = validar(p);
        try {
            Database.consultar(c -> {
                if (dao.existeCodigo(c, v.codigo(), v.id())) throw new NegocioException("Já existe outro produto com o código \"" + v.codigo() + "\".");
                dao.atualizar(c, v);
                return null;
            });
        } catch (DaoException e) {
            if (e.isDuplicado()) throw new NegocioException("Já existe outro produto com o código \"" + v.codigo() + "\".");
            throw e;
        }
    }

    /** Exclui fisicamente se nunca houve histórico; caso contrário apenas desativa (RN16). */
    public String excluirOuDesativar(int id) {
        Sessao.exigirAdmin();
        return Database.transacao(c -> {
            if (dao.possuiHistorico(c, id)) {
                dao.alterarAtivo(c, id, false);
                return "O produto possui vendas ou movimentações no histórico, por isso foi DESATIVADO (não poderá mais ser vendido).";
            }
            dao.excluir(c, id);
            return "Produto excluído definitivamente.";
        });
    }

    public void reativar(int id) {
        Sessao.exigirAdmin();
        Database.consultar(c -> { dao.alterarAtivo(c, id, true); return null; });
    }

    private Produto validar(Produto p) {
        String codigo = Validar.obrigatorio(p.codigo(), "Código");
        String nome = Validar.obrigatorio(p.nome(), "Nome");
        if (p.categoriaId() <= 0) throw new NegocioException("Selecione uma categoria.");
        if (p.preco() == null || p.preco().compareTo(BigDecimal.ZERO) < 0) throw new NegocioException("O preço não pode ser negativo.");
        if (p.quantidade() < 0) throw new NegocioException("A quantidade em estoque não pode ser negativa.");
        if (p.estoqueMinimo() < 0) throw new NegocioException("O estoque mínimo não pode ser negativo.");
        return new Produto(p.id(), codigo, nome, Validar.opcional(p.descricao()), Validar.opcional(p.marca()),
                Validar.opcional(p.modelo()), p.categoriaId(), p.categoriaNome(), p.preco(), p.quantidade(),
                p.estoqueMinimo(), p.ativo());
    }
}
