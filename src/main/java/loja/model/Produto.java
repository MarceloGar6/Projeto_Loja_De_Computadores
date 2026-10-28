package loja.model;

import java.math.BigDecimal;

public record Produto(int id, String codigo, String nome, String descricao, String marca, String modelo,
                      int categoriaId, String categoriaNome, BigDecimal preco,
                      int quantidade, int estoqueMinimo, boolean ativo) {

    /** Regra RN12: baixo = quantidade > 0 e <= mínimo; sem estoque = 0. */
    public String situacaoEstoque() {
        if (quantidade <= 0) return "Sem estoque";
        if (quantidade <= estoqueMinimo) return "Estoque baixo";
        return "Disponível";
    }

    @Override public String toString() { return codigo + " - " + nome; }
}
