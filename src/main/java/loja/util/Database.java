package loja.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Conexão com o MySQL (configurada em src/main/resources/db.properties) e helpers de transação. */
public final class Database {
    @FunctionalInterface
    public interface Trabalho<T> { T executar(Connection c) throws SQLException; }

    private static final Properties CONFIG = new Properties();

    static {
        // Usa db.properties (config local, fora do GitHub); se não existir, cai no modelo db.properties.example.
        try (InputStream in = abrirConfig()) {
            CONFIG.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler db.properties.", e);
        }
    }

    private static InputStream abrirConfig() {
        InputStream in = Database.class.getResourceAsStream("/db.properties");
        if (in == null) in = Database.class.getResourceAsStream("/db.properties.example");
        if (in == null) throw new IllegalStateException("Arquivo db.properties não encontrado em src/main/resources.");
        return in;
    }

    private Database() { }

    public static Connection abrir() throws SQLException {
        return DriverManager.getConnection(CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.user"), CONFIG.getProperty("db.password"));
    }

    /** Consulta/operação simples (uma conexão, sem transação explícita). */
    public static <T> T consultar(Trabalho<T> trabalho) {
        try (Connection c = abrir()) {
            return trabalho.executar(c);
        } catch (SQLException e) {
            throw new DaoException(e);
        }
    }

    /** Executa tudo ou nada: commit se terminar bem, rollback em qualquer erro (RN07, RN13). */
    public static <T> T transacao(Trabalho<T> trabalho) {
        try (Connection c = abrir()) {
            c.setAutoCommit(false);
            try {
                T resultado = trabalho.executar(c);
                c.commit();
                return resultado;
            } catch (SQLException e) {
                c.rollback();
                throw new DaoException(e);
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        }
    }
}
