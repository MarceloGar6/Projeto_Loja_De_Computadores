package loja.model;

/** Perfis de acesso do sistema. */
public enum TipoUsuario {
    ADMIN("Administrador"), VENDEDOR("Vendedor");

    private final String rotulo;
    TipoUsuario(String rotulo) { this.rotulo = rotulo; }
    @Override public String toString() { return rotulo; }
}
