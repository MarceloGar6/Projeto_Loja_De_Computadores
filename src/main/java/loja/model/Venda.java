package loja.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Venda(int id, int vendedorId, String vendedorNome, LocalDateTime dataHora, BigDecimal total,
                    FormaPagamento forma, SituacaoVenda situacao, LocalDateTime canceladaEm,
                    String canceladaPor, List<ItemVenda> itens) { }
