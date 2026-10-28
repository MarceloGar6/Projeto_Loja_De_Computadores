package loja.model;

import java.time.LocalDateTime;

/** Dados públicos de um usuário (o hash da senha nunca fica nesta entidade). */
public record Usuario(int id, String nomeCompleto, String nomeUsuario, String email,
                      TipoUsuario tipo, boolean ativo, LocalDateTime criadoEm) {
    public boolean isAdmin() { return tipo == TipoUsuario.ADMIN; }
    @Override public String toString() { return nomeCompleto; }
}
