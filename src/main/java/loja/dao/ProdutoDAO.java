package loja.dao;

import loja.model.Produto;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdutoDAO {

    private static final String BASE =
            "SELECT p.id, p.codigo, p.nome, p.descricao, p.marca, p.modelo, p.categoria_id, c.nome AS categoria_nome, "
          + "p.preco_venda, p.quantidade, p.estoque_minimo, p.ativo "
          + "FROM produtos p JOIN categorias c ON c.id = p.categoria_id ";

    private Produto mapear(ResultSet rs) throws SQLException {
        return new Produto(rs.getInt("id"), rs.getString("codigo"), rs.getString("nome"), rs.getString("descricao"),
                rs.getString("marca"), rs.getString("modelo"), rs.getInt("categoria_id"), rs.getString("categoria_nome"),
                rs.getBigDecimal("preco_venda"), rs.getInt("quantidade"), rs.getInt("estoque_minimo"), rs.getBoolean("ativo"));
    }

    /**
     * Pesquisa com filtros opcionais.
     * @param busca texto livre (nome, código ou categoria); pode ser null
     * @param categoriaId filtro de categoria; null = todas
     * @param situacao "Disponível", "Estoque baixo" ou "Sem estoque"; null = todas
     * @param apenasAtivos true = somente produtos ativos
     */
    public List<Produto> pesquisar(Connection c, String busca, Integer categoriaId, String situacao,
                                   boolean apenasAtivos) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (busca != null && !busca.isBlank()) {
            sql.append("AND (p.nome LIKE ? OR p.codigo LIKE ? OR c.nome LIKE ?) ");
            String like = "%" + busca.trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (categoriaId != null) { sql.append("AND p.categoria_id = ? "); params.add(categoriaId); }
        if ("Sem estoque".equals(situacao)) sql.append("AND p.quantidade = 0 ");
        else if ("Estoque baixo".equals(situacao)) sql.append("AND p.quantidade > 0 AND p.quantidade <= p.estoque_minimo ");
        else if ("Disponível".equals(situacao)) sql.append("AND p.quantidade > p.estoque_minimo ");
        if (apenasAtivos) sql.append("AND p.ativo = TRUE ");
        sql.append("ORDER BY p.nome");

        List<Produto> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Optional<Produto> buscarPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(BASE + "WHERE p.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapear(rs)) : Optional.empty(); }
        }
    }

    /** Trava a linha do produto até o fim da transação: evita que duas vendas usem o mesmo estoque. */
    public Optional<Produto> buscarParaAtualizar(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(BASE + "WHERE p.id = ? FOR UPDATE OF p")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapear(rs)) : Optional.empty(); }
        }
    }

    public boolean existeCodigo(Connection c, String codigo, int ignorarId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM produtos WHERE codigo = ? AND id <> ?")) {
            ps.setString(1, codigo);
            ps.setInt(2, ignorarId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        }
    }

    /** @return id gerado */
    public int inserir(Connection c, Produto p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO produtos (codigo, nome, descricao, marca, modelo, categoria_id, preco_venda, quantidade, estoque_minimo, ativo) "
              + "VALUES (?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.codigo());
            ps.setString(2, p.nome());
            ps.setString(3, p.descricao());
            ps.setString(4, p.marca());
            ps.setString(5, p.modelo());
            ps.setInt(6, p.categoriaId());
            ps.setBigDecimal(7, p.preco());
            ps.setInt(8, p.quantidade());
            ps.setInt(9, p.estoqueMinimo());
            ps.setBoolean(10, p.ativo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** Edição cadastral: NÃO altera a quantidade em estoque (isso é feito só pelo módulo de estoque). */
    public void atualizar(Connection c, Produto p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE produtos SET codigo=?, nome=?, descricao=?, marca=?, modelo=?, categoria_id=?, preco_venda=?, estoque_minimo=? WHERE id=?")) {
            ps.setString(1, p.codigo());
            ps.setString(2, p.nome());
            ps.setString(3, p.descricao());
            ps.setString(4, p.marca());
            ps.setString(5, p.modelo());
            ps.setInt(6, p.categoriaId());
            ps.setBigDecimal(7, p.preco());
            ps.setInt(8, p.estoqueMinimo());
            ps.setInt(9, p.id());
            ps.executeUpdate();
        }
    }

    public void atualizarQuantidade(Connection c, int id, int novaQuantidade) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE produtos SET quantidade=? WHERE id=?")) {
            ps.setInt(1, novaQuantidade);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void alterarAtivo(Connection c, int id, boolean ativo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE produtos SET ativo=? WHERE id=?")) {
            ps.setBoolean(1, ativo);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** O produto já apareceu em alguma venda ou movimentação de estoque? */
    public boolean possuiHistorico(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT (SELECT COUNT(*) FROM itens_venda WHERE produto_id = ?) "
              + "     + (SELECT COUNT(*) FROM movimentacoes_estoque WHERE produto_id = ?)")) {
            ps.setInt(1, id);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        }
    }

    public void excluir(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM produtos WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Contadores do painel: {cadastrados, disponíveis, estoque baixo, sem estoque}. */
    public int[] contadores(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*), "
              + "SUM(quantidade > estoque_minimo), "
              + "SUM(quantidade > 0 AND quantidade <= estoque_minimo), "
              + "SUM(quantidade = 0) FROM produtos WHERE ativo = TRUE");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)};
        }
    }
}
