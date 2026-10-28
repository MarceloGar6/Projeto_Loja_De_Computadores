package loja.service;

import loja.dao.MovimentacaoDAO;
import loja.dao.ProdutoDAO;
import loja.dao.VendaDAO;
import loja.model.*;
import loja.util.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class VendaService {
    private final VendaDAO vendaDao = new VendaDAO();
    private final ProdutoDAO produtoDao = new ProdutoDAO();
    private final MovimentacaoDAO movDao = new MovimentacaoDAO();

    /**
     * Confirma a venda. Tudo acontece em UMA transação (RN07/RN13):
     * valida estoque (com a linha do produto travada), grava venda + itens, baixa o estoque e registra as saídas.
     * Qualquer falha desfaz tudo.
     */
    public Venda confirmar(Carrinho carrinho, FormaPagamento forma) {
        Usuario vendedor = Sessao.exigirLogado();
        if (forma == null) throw new NegocioException("Selecione a forma de pagamento.");
        if (carrinho.vazio()) throw new NegocioException("Adicione ao menos um produto à venda.");

        carrinho.iniciarProcessamento();
        boolean sucesso = false;
        try {
            Venda venda = Database.transacao(c -> {
                if (vendaDao.existeToken(c, carrinho.token())) throw new NegocioException("Esta venda já foi registrada.");

                // Ordena por id para travar as linhas sempre na mesma ordem (evita deadlock entre vendas simultâneas).
                List<Carrinho.Linha> ordenadas = carrinho.linhas().stream()
                        .sorted(Comparator.comparingInt(l -> l.produto().id())).toList();

                // 1ª passada: trava, valida e calcula com os dados ATUAIS do banco.
                BigDecimal total = BigDecimal.ZERO;
                java.util.Map<Integer, Produto> atuais = new java.util.HashMap<>();
                for (Carrinho.Linha l : ordenadas) {
                    Produto p = produtoDao.buscarParaAtualizar(c, l.produto().id())
                            .orElseThrow(() -> new NegocioException("Um dos produtos não existe mais."));
                    if (!p.ativo()) throw new NegocioException("O produto \"" + p.nome() + "\" foi desativado e não pode ser vendido.");
                    if (p.quantidade() < l.quantidade())
                        throw new NegocioException("Estoque insuficiente para \"" + p.nome() + "\". Disponível agora: " + p.quantidade() + ".");
                    if (p.preco().compareTo(l.produto().preco()) != 0)
                        throw new NegocioException("O preço de \"" + p.nome() + "\" foi alterado para " + Fmt.moeda(p.preco())
                                + ". Remova o item e adicione-o novamente.");
                    atuais.put(p.id(), p);
                    total = total.add(p.preco().multiply(BigDecimal.valueOf(l.quantidade())));
                }

                // 2ª passada: grava.
                int vendaId = vendaDao.inserirVenda(c, vendedor.id(), total, forma, carrinho.token());
                for (Carrinho.Linha l : ordenadas) {
                    Produto p = atuais.get(l.produto().id());
                    BigDecimal subtotal = p.preco().multiply(BigDecimal.valueOf(l.quantidade()));
                    vendaDao.inserirItem(c, vendaId, p.id(), l.quantidade(), p.preco(), subtotal);   // preço histórico (RN08)
                    produtoDao.atualizarQuantidade(c, p.id(), p.quantidade() - l.quantidade());
                    movDao.inserir(c, p.id(), vendedor.id(), vendaId, TipoMovimentacao.SAIDA_VENDA, l.quantidade(),
                            "Venda #" + vendaId);
                }
                return vendaDao.buscarPorId(c, vendaId, false).orElseThrow();
            });
            sucesso = true;
            return venda;
        } finally {
            carrinho.terminarProcessamento(sucesso);
        }
    }

    /** Cancelamento administrativo: preserva a venda, devolve o estoque e registra as entradas (RN10). */
    public void cancelar(int vendaId) {
        Usuario admin = Sessao.exigirAdmin();
        Database.transacao(c -> {
            Venda v = vendaDao.buscarPorId(c, vendaId, true).orElseThrow(() -> new NegocioException("Venda não encontrada."));
            if (v.situacao() == SituacaoVenda.CANCELADA) throw new NegocioException("Esta venda já foi cancelada.");
            vendaDao.marcarCancelada(c, vendaId, admin.id());
            List<ItemVenda> itens = v.itens().stream().sorted(Comparator.comparingInt(ItemVenda::produtoId)).toList();
            for (ItemVenda i : itens) {
                Produto p = produtoDao.buscarParaAtualizar(c, i.produtoId()).orElseThrow();
                produtoDao.atualizarQuantidade(c, p.id(), p.quantidade() + i.quantidade());
                movDao.inserir(c, p.id(), admin.id(), vendaId, TipoMovimentacao.ENTRADA_CANCELAMENTO, i.quantidade(),
                        "Cancelamento da venda #" + vendaId);
            }
            return null;
        });
    }

    /** Vendedor enxerga apenas as próprias vendas; o filtro é imposto aqui, não na tela. */
    public List<Venda> pesquisar(Integer numero, LocalDate de, LocalDate ate, Integer vendedorId, SituacaoVenda situacao) {
        Usuario u = Sessao.exigirLogado();
        Integer filtroVendedor = u.isAdmin() ? vendedorId : Integer.valueOf(u.id());
        return Database.consultar(c -> vendaDao.pesquisar(c, numero, de, ate, filtroVendedor, situacao));
    }

    public Venda detalhes(int vendaId) {
        Usuario u = Sessao.exigirLogado();
        Venda v = Database.consultar(c -> vendaDao.buscarPorId(c, vendaId, false))
                .orElseThrow(() -> new NegocioException("Venda não encontrada."));
        if (!u.isAdmin() && v.vendedorId() != u.id())
            throw new NegocioException("Você só pode consultar as suas próprias vendas.");
        return v;
    }
}
