package loja.ui;

import loja.model.FormaPagamento;
import loja.model.Produto;
import loja.model.Venda;
import loja.service.Carrinho;
import loja.service.ProdutoService;
import loja.service.VendaService;
import loja.util.Fmt;
import loja.util.NegocioException;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Tela de vendas: o vendedor monta a venda; os valores são calculados pelo sistema. */
public class PainelVendas extends JPanel implements Atualizavel {
    private final ProdutoService produtoService = new ProdutoService();
    private final VendaService vendaService = new VendaService();
    private Carrinho carrinho = new Carrinho();

    private final JTextField txtBusca = new JTextField(14);
    private final ModeloTabela modeloProdutos = new ModeloTabela("Código", "Produto", "Categoria", "Preço", "Disponível");
    private final JTable tabelaProdutos = UI.tabela(modeloProdutos);
    private List<Produto> produtos = List.of();
    private final JSpinner spnQtd = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));

    private final ModeloTabela modeloCarrinho = new ModeloTabela("Código", "Produto", "Qtd", "Preço unit.", "Subtotal");
    private final JTable tabelaCarrinho = UI.tabela(modeloCarrinho);
    private final JLabel lblTotal = new JLabel("Total: R$ 0,00");
    private final JComboBox<FormaPagamento> cmbForma = new JComboBox<>(FormaPagamento.values());
    private final JButton btnFinalizar;

    public PainelVendas() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ---- esquerda: produtos disponíveis ----
        JPanel esquerda = new JPanel(new BorderLayout(0, 6));
        esquerda.setBorder(BorderFactory.createTitledBorder("Produtos disponíveis"));
        txtBusca.addActionListener(e -> UI.executar(this, this::carregarProdutos));
        esquerda.add(UI.linha(new JLabel("Pesquisar:"), txtBusca, UI.botao("Buscar", this::carregarProdutos)), BorderLayout.NORTH);
        esquerda.add(new JScrollPane(tabelaProdutos), BorderLayout.CENTER);
        esquerda.add(UI.linha(new JLabel("Quantidade:"), spnQtd, UI.botao("Adicionar à venda ➜", this::adicionar)), BorderLayout.SOUTH);

        // ---- direita: itens da venda ----
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 20f));
        btnFinalizar = UI.botao("Finalizar venda", this::finalizar);
        JPanel direita = new JPanel(new BorderLayout(0, 6));
        direita.setBorder(BorderFactory.createTitledBorder("Venda atual"));
        direita.add(new JScrollPane(tabelaCarrinho), BorderLayout.CENTER);
        JPanel rodape = new JPanel(new GridLayout(0, 1));
        rodape.add(UI.linha(lblTotal));
        rodape.add(UI.linha(new JLabel("Forma de pagamento:"), cmbForma));
        rodape.add(UI.linha(UI.botao("Remover item", this::removerItem), UI.botao("Cancelar venda", this::cancelarOperacao), btnFinalizar));
        direita.add(rodape, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, esquerda, direita);
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void atualizar() {
        UI.executar(this, () -> { carregarProdutos(); atualizarCarrinho(); });
    }

    private void carregarProdutos() {
        produtos = produtoService.pesquisarParaVenda(txtBusca.getText());
        modeloProdutos.setDados(produtos.stream().map(p -> new Object[]{p.codigo(), p.nome(), p.categoriaNome(),
                Fmt.moeda(p.preco()), p.quantidade()}).toList());
    }

    private void atualizarCarrinho() {
        modeloCarrinho.setDados(carrinho.linhas().stream().map(l -> new Object[]{l.produto().codigo(), l.produto().nome(),
                l.quantidade(), Fmt.moeda(l.produto().preco()), Fmt.moeda(l.subtotal())}).toList());
        lblTotal.setText("Total: " + Fmt.moeda(carrinho.total()));
    }

    private void adicionar() {
        Produto p = produtos.get(UI.selecionada(tabelaProdutos));
        carrinho.adicionar(p, (Integer) spnQtd.getValue());
        spnQtd.setValue(1);
        atualizarCarrinho();
    }

    private void removerItem() {
        int idx = UI.selecionada(tabelaCarrinho);
        carrinho.remover(carrinho.linhas().get(idx).produto().id());
        atualizarCarrinho();
    }

    private void cancelarOperacao() {
        if (carrinho.vazio()) return;
        if (UI.confirmar(this, "Descartar a venda em andamento?")) {
            carrinho = new Carrinho();
            atualizarCarrinho();
        }
    }

    private void finalizar() {
        if (carrinho.vazio()) throw new NegocioException("Adicione ao menos um produto à venda.");
        FormaPagamento forma = (FormaPagamento) cmbForma.getSelectedItem();

        StringBuilder resumo = new StringBuilder("RESUMO DA VENDA\n\n");
        for (Carrinho.Linha l : carrinho.linhas())
            resumo.append(String.format("%-28s %3d x %10s = %11s%n", cortar(l.produto().nome()), l.quantidade(),
                    Fmt.moeda(l.produto().preco()), Fmt.moeda(l.subtotal())));
        resumo.append("\nTOTAL: ").append(Fmt.moeda(carrinho.total()));
        resumo.append("\nPagamento: ").append(forma).append("\n\nConfirmar a venda?");
        if (!UI.confirmarTexto(this, "Confirmar venda", resumo.toString())) return;

        btnFinalizar.setEnabled(false);   // evita cliques repetidos (RN11); o Carrinho também bloqueia duplicidade
        try {
            Venda venda = vendaService.confirmar(carrinho, forma);
            carrinho = new Carrinho();
            atualizarCarrinho();
            carregarProdutos();
            Comprovante.mostrar(this, venda);
        } finally {
            btnFinalizar.setEnabled(true);
            try { carregarProdutos(); } catch (RuntimeException ignorado) { /* a lista será recarregada na próxima busca */ }
        }
    }

    private static String cortar(String s) { return s.length() > 28 ? s.substring(0, 27) + "…" : s; }
}
