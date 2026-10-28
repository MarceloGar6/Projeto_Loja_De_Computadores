package loja.dao;

import loja.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VendaDAO {

    private static final String BASE =
            "SELECT v.id, v.vendedor_id, u.nome_completo AS vendedor, v.data_hora, v.valor_total, v.forma_pagamento, "
          + "v.situacao, v.cancelada_em, uc.nome_completo AS cancelada_por "
          + "FROM vendas v JOIN usuarios u ON u.id = v.vendedor_id LEFT JOIN usuarios uc ON uc.id = v.cancelada_por ";

    private Venda mapear(ResultSet rs, List<ItemVenda> itens) throws SQLException {
        Timestamp cancelada = rs.getTimestamp("cancelada_em");
        return new Venda(rs.getInt("id"), rs.getInt("vendedor_id"), rs.getString("vendedor"),
                rs.getTimestamp("data_hora").toLocalDateTime(), rs.getBigDecimal("valor_total"),
                FormaPagamento.valueOf(rs.getString("forma_pagamento")), SituacaoVenda.valueOf(rs.getString("situacao")),
                cancelada == null ? null : cancelada.toLocalDateTime(), rs.getString("cancelada_por"), itens);
    }

    public boolean existeToken(Connection c, String token) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM vendas WHERE token = ?")) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        }
    }

    /** @return id da venda criada */
    public int inserirVenda(Connection c, int vendedorId, BigDecimal total, FormaPagamento forma, String token) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO vendas (vendedor_id, valor_total, forma_pagamento, situacao, token) VALUES (?,?,?, 'CONCLUIDA', ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, vendedorId);
            ps.setBigDecimal(2, total);
            ps.setString(3, forma.name());
            ps.setString(4, token);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        }
    }

    public void inserirItem(Connection c, int vendaId, int produtoId, int quantidade, BigDecimal preco, BigDecimal subtotal)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO itens_venda (venda_id, produto_id, quantidade, preco_unitario, subtotal) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, vendaId);
            ps.setInt(2, produtoId);
            ps.setInt(3, quantidade);
            ps.setBigDecimal(4, preco);
            ps.setBigDecimal(5, subtotal);
            ps.executeUpdate();
        }
    }

    public List<ItemVenda> itens(Connection c, int vendaId) throws SQLException {
        List<ItemVenda> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT i.id, i.produto_id, p.codigo, p.nome, i.quantidade, i.preco_unitario, i.subtotal "
              + "FROM itens_venda i JOIN produtos p ON p.id = i.produto_id WHERE i.venda_id = ? ORDER BY i.id")) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    lista.add(new ItemVenda(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                            rs.getInt(5), rs.getBigDecimal(6), rs.getBigDecimal(7)));
            }
        }
        return lista;
    }

    public Optional<Venda> buscarPorId(Connection c, int id, boolean travar) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(BASE + "WHERE v.id = ?" + (travar ? " FOR UPDATE OF v" : ""))) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapear(rs, itens(c, id)));
            }
        }
    }

    public void marcarCancelada(Connection c, int vendaId, int adminId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE vendas SET situacao='CANCELADA', cancelada_por=?, cancelada_em=NOW() WHERE id=? AND situacao='CONCLUIDA'")) {
            ps.setInt(1, adminId);
            ps.setInt(2, vendaId);
            ps.executeUpdate();
        }
    }

    public List<Venda> pesquisar(Connection c, Integer numero, LocalDate de, LocalDate ate, Integer vendedorId,
                                 SituacaoVenda situacao) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (numero != null) { sql.append("AND v.id = ? "); params.add(numero); }
        if (de != null) { sql.append("AND v.data_hora >= ? "); params.add(Timestamp.valueOf(de.atStartOfDay())); }
        if (ate != null) { sql.append("AND v.data_hora < ? "); params.add(Timestamp.valueOf(ate.plusDays(1).atStartOfDay())); }
        if (vendedorId != null) { sql.append("AND v.vendedor_id = ? "); params.add(vendedorId); }
        if (situacao != null) { sql.append("AND v.situacao = ? "); params.add(situacao.name()); }
        sql.append("ORDER BY v.data_hora DESC, v.id DESC LIMIT 1000");

        List<Venda> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs, List.of()));
            }
        }
        return lista;
    }

    // ------------------------- Relatórios (somente vendas CONCLUÍDAS entram nos totais) -------------------------

    /** {quantidade de vendas concluídas, valor total concluído, quantidade de canceladas}. */
    public Object[] resumo(Connection c, LocalDate de, LocalDate ate, Integer vendedorId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(SUM(situacao='CONCLUIDA'),0), COALESCE(SUM(CASE WHEN situacao='CONCLUIDA' THEN valor_total ELSE 0 END),0), "
              + "COALESCE(SUM(situacao='CANCELADA'),0) FROM vendas WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        filtrosPeriodo(sql, params, de, ate, vendedorId, "");
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Object[]{rs.getInt(1), rs.getBigDecimal(2), rs.getInt(3)};
            }
        }
    }

    /** Linhas: {código, produto, unidades vendidas, valor vendido}. */
    public List<Object[]> maisVendidos(Connection c, LocalDate de, LocalDate ate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT p.codigo, p.nome, SUM(i.quantidade), SUM(i.subtotal) FROM itens_venda i "
              + "JOIN vendas v ON v.id = i.venda_id JOIN produtos p ON p.id = i.produto_id "
              + "WHERE v.situacao = 'CONCLUIDA' ");
        List<Object> params = new ArrayList<>();
        filtrosPeriodo(sql, params, de, ate, null, "v.");
        sql.append("GROUP BY p.id, p.codigo, p.nome ORDER BY SUM(i.quantidade) DESC LIMIT 10");
        return linhas(c, sql, params, 4);
    }

    /** Linhas: {vendedor, quantidade de vendas, valor total}. */
    public List<Object[]> porVendedor(Connection c, LocalDate de, LocalDate ate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT u.nome_completo, COUNT(*), SUM(v.valor_total) FROM vendas v JOIN usuarios u ON u.id = v.vendedor_id "
              + "WHERE v.situacao = 'CONCLUIDA' ");
        List<Object> params = new ArrayList<>();
        filtrosPeriodo(sql, params, de, ate, null, "v.");
        sql.append("GROUP BY u.id, u.nome_completo ORDER BY SUM(v.valor_total) DESC");
        return linhas(c, sql, params, 3);
    }

    private void filtrosPeriodo(StringBuilder sql, List<Object> params, LocalDate de, LocalDate ate,
                                Integer vendedorId, String prefixo) {
        if (de != null) { sql.append("AND ").append(prefixo).append("data_hora >= ? "); params.add(Timestamp.valueOf(de.atStartOfDay())); }
        if (ate != null) { sql.append("AND ").append(prefixo).append("data_hora < ? "); params.add(Timestamp.valueOf(ate.plusDays(1).atStartOfDay())); }
        if (vendedorId != null) { sql.append("AND ").append(prefixo).append("vendedor_id = ? "); params.add(vendedorId); }
    }

    private List<Object[]> linhas(Connection c, StringBuilder sql, List<Object> params, int colunas) throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] linha = new Object[colunas];
                    for (int i = 0; i < colunas; i++) linha[i] = rs.getObject(i + 1);
                    lista.add(linha);
                }
            }
        }
        return lista;
    }
}
