package loja.dao;

import loja.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acesso à tabela usuarios. Todas as consultas são parametrizadas (RN14). */
public class UsuarioDAO {

    public record Credencial(Usuario usuario, String senhaHash) { }

    private static final String BASE =
            "SELECT id, nome_completo, nome_usuario, email, senha_hash, tipo, ativo, criado_em FROM usuarios ";

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(rs.getInt("id"), rs.getString("nome_completo"), rs.getString("nome_usuario"),
                rs.getString("email"), TipoUsuario.valueOf(rs.getString("tipo")), rs.getBoolean("ativo"),
                rs.getTimestamp("criado_em").toLocalDateTime());
    }

    public Optional<Credencial> buscarPorLogin(Connection c, String login) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(BASE + "WHERE nome_usuario = ? OR email = ?")) {
            ps.setString(1, login);
            ps.setString(2, login);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new Credencial(mapear(rs), rs.getString("senha_hash"))) : Optional.empty();
            }
        }
    }

    public Optional<Usuario> buscarPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(BASE + "WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<Usuario> listar(Connection c) throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(BASE + "ORDER BY nome_completo");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public int contar(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM usuarios");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public int contarAdminsAtivos(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM usuarios WHERE tipo='ADMIN' AND ativo=TRUE");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public boolean existe(Connection c, String nomeUsuario, String email, int ignorarId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM usuarios WHERE (nome_usuario = ? OR email = ?) AND id <> ?")) {
            ps.setString(1, nomeUsuario);
            ps.setString(2, email);
            ps.setInt(3, ignorarId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public void inserir(Connection c, String nomeCompleto, String nomeUsuario, String email,
                        String senhaHash, TipoUsuario tipo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO usuarios (nome_completo, nome_usuario, email, senha_hash, tipo) VALUES (?,?,?,?,?)")) {
            ps.setString(1, nomeCompleto);
            ps.setString(2, nomeUsuario);
            ps.setString(3, email);
            ps.setString(4, senhaHash);
            ps.setString(5, tipo.name());
            ps.executeUpdate();
        }
    }

    public void atualizar(Connection c, int id, String nomeCompleto, String nomeUsuario, String email,
                          TipoUsuario tipo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE usuarios SET nome_completo=?, nome_usuario=?, email=?, tipo=? WHERE id=?")) {
            ps.setString(1, nomeCompleto);
            ps.setString(2, nomeUsuario);
            ps.setString(3, email);
            ps.setString(4, tipo.name());
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    public void atualizarSenha(Connection c, int id, String senhaHash) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE usuarios SET senha_hash=? WHERE id=?")) {
            ps.setString(1, senhaHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void alterarAtivo(Connection c, int id, boolean ativo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE usuarios SET ativo=? WHERE id=?")) {
            ps.setBoolean(1, ativo);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
