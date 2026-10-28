package loja.dao;

import loja.model.Categoria;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    public List<Categoria> listar(Connection c, boolean apenasAtivas) throws SQLException {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT id, nome, descricao, ativo FROM categorias "
                + (apenasAtivas ? "WHERE ativo = TRUE " : "") + "ORDER BY nome";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new Categoria(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)));
        }
        return lista;
    }

    public boolean existeNome(Connection c, String nome, int ignorarId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM categorias WHERE nome = ? AND id <> ?")) {
            ps.setString(1, nome);
            ps.setInt(2, ignorarId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        }
    }

    public void inserir(Connection c, String nome, String descricao) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO categorias (nome, descricao) VALUES (?, ?)")) {
            ps.setString(1, nome);
            ps.setString(2, descricao);
            ps.executeUpdate();
        }
    }

    public void atualizar(Connection c, int id, String nome, String descricao) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE categorias SET nome=?, descricao=? WHERE id=?")) {
            ps.setString(1, nome);
            ps.setString(2, descricao);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void alterarAtivo(Connection c, int id, boolean ativo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE categorias SET ativo=? WHERE id=?")) {
            ps.setBoolean(1, ativo);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
