package loja.service;

import loja.dao.MovimentacaoDAO;
import loja.dao.ProdutoDAO;
import loja.model.Movimentacao;
import loja.model.Produto;
import loja.model.TipoMovimentacao;
import loja.model.Usuario;
import loja.util.*;

import java.time.LocalDate;
import java.util.List;

public class EstoqueService {
    private final ProdutoDAO produtoDao = new ProdutoDAO();
    private final MovimentacaoDAO movDao = new MovimentacaoDAO();

    /** Entrada de mercadoria: atualiza a quantidade e grava a movimentação na mesma transação. */
    public void registrarEntrada(int produtoId, int quantidade, String observacao) {
        Usuario admin = Sessao.exigirAdmin();
        if (quantidade <= 0) throw new NegocioException("A quantidade recebida deve ser maior que zero.");
        Database.transacao(c -> {
            Produto p = produtoDao.buscarParaAtualizar(c, produtoId)
                    .orElseThrow(() -> new NegocioException("Produto não encontrado."));
            produtoDao.atualizarQuantidade(c, produtoId, p.quantidade() + quantidade);
            movDao.inserir(c, produtoId, admin.id(), null, TipoMovimentacao.ENTRADA, quantidade, Validar.opcional(observacao));
            return null;
        });
    }

    public List<Movimentacao> movimentacoes(Integer produtoId, LocalDate de, LocalDate ate) {
        Sessao.exigirAdmin();
        return Database.consultar(c -> movDao.listar(c, produtoId, de, ate));
    }
}
