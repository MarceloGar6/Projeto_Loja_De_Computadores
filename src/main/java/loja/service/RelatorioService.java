package loja.service;

import loja.dao.ProdutoDAO;
import loja.dao.VendaDAO;
import loja.model.Usuario;
import loja.util.Database;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class RelatorioService {
    private final VendaDAO vendaDao = new VendaDAO();
    private final ProdutoDAO produtoDao = new ProdutoDAO();

    public record Painel(int cadastrados, int disponiveis, int baixo, int semEstoque, int vendasHoje, BigDecimal valorHoje) { }

    public record Resumo(int vendas, BigDecimal valor, int canceladas) { }

    /** Indicadores do painel. Para vendedor, os números de vendas consideram só as vendas dele. */
    public Painel painel() {
        Usuario u = Sessao.exigirLogado();
        return Database.consultar(c -> {
            int[] p = u.isAdmin() ? produtoDao.contadores(c) : new int[4];
            Object[] r = vendaDao.resumo(c, LocalDate.now(), LocalDate.now(), u.isAdmin() ? null : u.id());
            return new Painel(p[0], p[1], p[2], p[3], (Integer) r[0], (BigDecimal) r[1]);
        });
    }

    public Resumo resumo(LocalDate de, LocalDate ate) {
        Sessao.exigirAdmin();
        return Database.consultar(c -> {
            Object[] r = vendaDao.resumo(c, de, ate, null);
            return new Resumo((Integer) r[0], (BigDecimal) r[1], (Integer) r[2]);
        });
    }

    public List<Object[]> maisVendidos(LocalDate de, LocalDate ate) {
        Sessao.exigirAdmin();
        return Database.consultar(c -> vendaDao.maisVendidos(c, de, ate));
    }

    public List<Object[]> porVendedor(LocalDate de, LocalDate ate) {
        Sessao.exigirAdmin();
        return Database.consultar(c -> vendaDao.porVendedor(c, de, ate));
    }
}
