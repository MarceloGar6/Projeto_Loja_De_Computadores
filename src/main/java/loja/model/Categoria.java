package loja.model;

public record Categoria(int id, String nome, String descricao, boolean ativo) {
    @Override public String toString() { return nome; }
}
