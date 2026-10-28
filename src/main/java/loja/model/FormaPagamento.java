package loja.model;

public enum FormaPagamento {
    DINHEIRO("Dinheiro"), DEBITO("Cartão de débito"), CREDITO("Cartão de crédito"), PIX("PIX");

    private final String rotulo;
    FormaPagamento(String rotulo) { this.rotulo = rotulo; }
    @Override public String toString() { return rotulo; }
}
