package loja.model;

import java.math.BigDecimal;

public record ItemVenda(int id, int produtoId, String codigo, String nomeProduto,
                        int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) { }
