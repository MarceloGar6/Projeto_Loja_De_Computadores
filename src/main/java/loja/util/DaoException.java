package loja.util;

import java.sql.SQLException;

/** Falha técnica de banco. O detalhe vai para o log (console); o usuário recebe uma mensagem genérica. */
public class DaoException extends RuntimeException {
    private final boolean duplicado;

    public DaoException(SQLException causa) {
        super("Não foi possível concluir a operação no banco de dados. Verifique a conexão e tente novamente.", causa);
        this.duplicado = causa.getErrorCode() == 1062;   // MySQL: chave duplicada
        causa.printStackTrace();
    }

    public boolean isDuplicado() { return duplicado; }
}
