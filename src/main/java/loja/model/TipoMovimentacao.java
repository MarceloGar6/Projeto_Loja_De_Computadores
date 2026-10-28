package loja.model;

public enum TipoMovimentacao {
    ENTRADA("Entrada de mercadoria"),
    SAIDA_VENDA("Saída por venda"),
    ENTRADA_CANCELAMENTO("Entrada por cancelamento");

    private final String rotulo;
    TipoMovimentacao(String rotulo) { this.rotulo = rotulo; }
    @Override public String toString() { return rotulo; }
}
