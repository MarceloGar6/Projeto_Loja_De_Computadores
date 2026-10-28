package loja.dao;

import loja.model.Movimentacao;
import loja.model.TipoMovimentacao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MovimentacaoDAO {

    public void inserir(Connection c, int produtoId, int usuarioId, Integer vendaId, TipoMovimentacao tipo,
                        int quantidade, String observacao) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO movimentacoes_estoque (produto_id, usuario_id, venda_id, tipo, quantidade, observacao) VALUES (?,?,?,?,?,?)")) {
            ps.setInt(1, produtoId);
            ps.setInt(2, usuarioId);
            if (vendaId == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, vendaId);
            ps.setString(4, tipo.name());
            ps.setInt(5, quantidade);
            ps.setString(6, observacao);
            ps.executeUpdate();
        }
    }

    public List<Movimentacao> listar(Connection c, Integer produtoId, LocalDate de, LocalDate ate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT m.id, m.produto_id, p.codigo, p.nome, u.nome_completo, m.venda_id, m.tipo, m.quantidade, m.data_hora, m.observacao "
              + "FROM movimentacoes_estoque m JOIN produtos p ON p.id = m.produto_id JOIN usuarios u ON u.id = m.usuario_id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (produtoId != null) { sql.append("AND m.produto_id = ? "); params.add(produtoId); }
        if (de != null) { sql.append("AND m.data_hora >= ? "); params.add(Timestamp.valueOf(de.atStartOfDay())); }
        if (ate != null) { sql.append("AND m.data_hora < ? "); params.add(Timestamp.valueOf(ate.plusDays(1).atStartOfDay())); }
        sql.append("ORDER BY m.data_hora DESC, m.id DESC LIMIT 1000");

        List<Movimentacao> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int vid = rs.getInt(6);
                    Integer vendaId = rs.wasNull() ? null : vid;
                    lista.add(new Movimentacao(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                            vendaId, TipoMovimentacao.valueOf(rs.getString(7)), rs.getInt(8),
                            rs.getTimestamp(9).toLocalDateTime(), rs.getString(10)));
                }
            }
        }
        return lista;
    }
}
