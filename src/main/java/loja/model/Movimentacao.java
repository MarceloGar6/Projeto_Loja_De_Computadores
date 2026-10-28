package loja.model;

import java.time.LocalDateTime;

public record Movimentacao(int id, int produtoId, String produtoCodigo, String produtoNome, String usuarioNome,
                           Integer vendaId, TipoMovimentacao tipo, int quantidade,
                           LocalDateTime dataHora, String observacao) { }
