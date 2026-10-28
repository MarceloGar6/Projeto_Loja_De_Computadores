package loja.model;

public enum SituacaoVenda {
    CONCLUIDA("Concluída"), CANCELADA("Cancelada");

    private final String rotulo;
    SituacaoVenda(String rotulo) { this.rotulo = rotulo; }
    @Override public String toString() { return rotulo; }
}
