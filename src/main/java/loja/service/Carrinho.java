package loja.service;

import loja.model.Produto;
import loja.util.NegocioException;

import java.math.BigDecimal;
import java.util.*;

/**
 * Venda em montagem (ainda não gravada no banco). Os valores são calculados aqui,
 * nunca pelo vendedor: subtotal = preço × quantidade; total = soma dos subtotais.
 */
public class Carrinho {

    public record Linha(Produto produto, int quantidade) {
        public BigDecimal subtotal() { return produto.preco().multiply(BigDecimal.valueOf(quantidade)); }
    }

    private final Map<Integer, Linha> linhas = new LinkedHashMap<>();
    private final String token = UUID.randomUUID().toString();   // identifica esta venda (RN11)
    private boolean emProcessamento;
    private boolean finalizada;

    public String token() { return token; }

    public void adicionar(Produto p, int quantidade) {
        if (quantidade <= 0) throw new NegocioException("Informe uma quantidade maior que zero.");
        if (!p.ativo()) throw new NegocioException("O produto \"" + p.nome() + "\" está inativo e não pode ser vendido.");
        int jaNoCarrinho = linhas.containsKey(p.id()) ? linhas.get(p.id()).quantidade() : 0;
        if (jaNoCarrinho + quantidade > p.quantidade())
            throw new NegocioException("Estoque insuficiente para \"" + p.nome() + "\". Disponível: " + p.quantidade()
                    + (jaNoCarrinho > 0 ? " (já há " + jaNoCarrinho + " na venda)." : "."));
        linhas.put(p.id(), new Linha(p, jaNoCarrinho + quantidade));
    }

    public void remover(int produtoId) { linhas.remove(produtoId); }
    public void limpar() { linhas.clear(); }
    public boolean vazio() { return linhas.isEmpty(); }
    public List<Linha> linhas() { return List.copyOf(linhas.values()); }

    public BigDecimal total() {
        return linhas.values().stream().map(Linha::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // controle anti-duplicidade (usado pelo VendaService)
    synchronized void iniciarProcessamento() {
        if (finalizada) throw new NegocioException("Esta venda já foi registrada.");
        if (emProcessamento) throw new NegocioException("Esta venda já está sendo processada. Aguarde.");
        emProcessamento = true;
    }
    synchronized void terminarProcessamento(boolean sucesso) {
        emProcessamento = false;
        if (sucesso) finalizada = true;
    }
}
