package loja.ui;

import loja.model.Movimentacao;
import loja.model.Produto;
import loja.service.EstoqueService;
import loja.service.ProdutoService;
import loja.util.Fmt;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/** Controle de estoque (administrador): posição atual, entrada de mercadorias e histórico de movimentações. */
public class PainelEstoque extends JPanel implements Atualizavel {
    private final ProdutoService produtoService = new ProdutoService();
    private final EstoqueService estoqueService = new EstoqueService();

    // aba "Posição atual"
    private final JTextField txtBusca = new JTextField(16);
    private final JComboBox<String> cmbSituacao = new JComboBox<>(new String[]{"Todas", "Disponível", "Estoque baixo", "Sem estoque"});
    private final ModeloTabela modeloPosicao = new ModeloTabela("Código", "Produto", "Categoria", "Quantidade", "Mínimo", "Situação");
    private final JTable tabelaPosicao = UI.tabela(modeloPosicao);
    private final JLabel lblAlerta = new JLabel(" ");
    private List<Produto> produtos = List.of();

    // aba "Histórico"
    private final JComboBox<Object> cmbProdutoHist = new JComboBox<>();
    private final JTextField txtDe = new JTextField(9), txtAte = new JTextField(9);
    private final ModeloTabela modeloHist = new ModeloTabela("Data/hora", "Produto", "Tipo", "Qtd", "Usuário", "Venda", "Observação");

    public PainelEstoque() {
        setLayout(new BorderLayout());
        UI.destacarSituacao(tabelaPosicao, 5);

        JPanel posicao = new JPanel(new BorderLayout(0, 8));
        posicao.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtBusca.addActionListener(e -> UI.executar(this, this::carregarPosicao));
        JPanel filtros = UI.linha(new JLabel("Produto/código:"), txtBusca, new JLabel("Situação:"), cmbSituacao,
                UI.botao("Filtrar", this::carregarPosicao));
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(filtros, BorderLayout.NORTH);
        lblAlerta.setForeground(new Color(0xB45F06));
        lblAlerta.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        norte.add(lblAlerta, BorderLayout.SOUTH);
        posicao.add(norte, BorderLayout.NORTH);
        posicao.add(new JScrollPane(tabelaPosicao), BorderLayout.CENTER);
        posicao.add(UI.linha(UI.botao("Registrar entrada de mercadoria", this::registrarEntrada)), BorderLayout.SOUTH);

        JPanel historico = new JPanel(new BorderLayout(0, 8));
        historico.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        historico.add(UI.linha(new JLabel("Produto:"), cmbProdutoHist, new JLabel("De (dd/mm/aaaa):"), txtDe,
                new JLabel("Até:"), txtAte, UI.botao("Consultar", this::carregarHistorico)), BorderLayout.NORTH);
        historico.add(new JScrollPane(UI.tabela(modeloHist)), BorderLayout.CENTER);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Posição atual", posicao);
        abas.addTab("Histórico de movimentações", historico);
        add(abas, BorderLayout.CENTER);
    }

    @Override
    public void atualizar() {
        UI.executar(this, () -> {
            carregarPosicao();
            Object sel = cmbProdutoHist.getSelectedItem();
            cmbProdutoHist.removeAllItems();
            cmbProdutoHist.addItem("Todos");
            for (Produto p : produtoService.pesquisar(null, null, null)) cmbProdutoHist.addItem(p);
            if (sel != null) cmbProdutoHist.setSelectedItem(sel);
            carregarHistorico();
        });
    }

    private void carregarPosicao() {
        String sit = "Todas".equals(cmbSituacao.getSelectedItem()) ? null : (String) cmbSituacao.getSelectedItem();
        produtos = produtoService.pesquisar(txtBusca.getText(), null, sit);
        modeloPosicao.setDados(produtos.stream().map(p -> new Object[]{p.codigo(), p.nome(), p.categoriaNome(),
                p.quantidade(), p.estoqueMinimo(), p.situacaoEstoque()}).toList());
        long baixos = produtos.stream().filter(p -> p.ativo() && p.quantidade() > 0 && p.quantidade() <= p.estoqueMinimo()).count();
        long zerados = produtos.stream().filter(p -> p.ativo() && p.quantidade() == 0).count();
        lblAlerta.setText(baixos + zerados == 0 ? " " : "⚠ Atenção: " + baixos + " produto(s) com estoque baixo e " + zerados + " sem estoque nesta lista.");
    }

    private void registrarEntrada() {
        List<Produto> todos = produtoService.pesquisar(null, null, null);
        if (todos.isEmpty()) throw new loja.util.NegocioException("Cadastre produtos antes de registrar entradas.");
        JComboBox<Produto> cmb = new JComboBox<>(todos.toArray(new Produto[0]));
        int linha = tabelaPosicao.getSelectedRow();
        if (linha >= 0) {
            Produto sel = produtos.get(tabelaPosicao.convertRowIndexToModel(linha));
            for (Produto p : todos) if (p.id() == sel.id()) cmb.setSelectedItem(p);
        }
        JTextField qtd = new JTextField(10), obs = new JTextField(22);
        Form f = new Form();
        f.campo("Produto:", cmb);
        f.campo("Quantidade recebida:", qtd);
        f.campo("Observação:", obs);
        if (JOptionPane.showConfirmDialog(this, f, "Entrada de mercadoria", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        Produto p = (Produto) cmb.getSelectedItem();
        int q = Fmt.lerInteiro(qtd.getText(), "Quantidade recebida");
        estoqueService.registrarEntrada(p.id(), q, obs.getText());
        UI.info(this, "Entrada registrada com sucesso.");
        atualizar();
    }

    private void carregarHistorico() {
        Integer produtoId = cmbProdutoHist.getSelectedItem() instanceof Produto p ? p.id() : null;
        LocalDate de = Fmt.lerData(txtDe.getText(), "De");
        LocalDate ate = Fmt.lerData(txtAte.getText(), "Até");
        List<Movimentacao> movs = estoqueService.movimentacoes(produtoId, de, ate);
        modeloHist.setDados(movs.stream().map(m -> new Object[]{Fmt.dataHora(m.dataHora()), m.produtoCodigo() + " - " + m.produtoNome(),
                m.tipo(), m.quantidade(), m.usuarioNome(), m.vendaId() == null ? "" : "#" + m.vendaId(),
                m.observacao() == null ? "" : m.observacao()}).toList());
    }
}
