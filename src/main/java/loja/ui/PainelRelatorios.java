package loja.ui;

import loja.service.RelatorioService;
import loja.util.Fmt;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Relatórios básicos (somente vendas concluídas entram nos totais). */
public class PainelRelatorios extends JPanel implements Atualizavel {
    private final RelatorioService service = new RelatorioService();
    private final JTextField txtDe = new JTextField(9), txtAte = new JTextField(9);
    private final JLabel lblResumo = new JLabel(" ");
    private final ModeloTabela modeloProdutos = new ModeloTabela("Código", "Produto", "Unidades vendidas", "Valor vendido");
    private final ModeloTabela modeloVendedores = new ModeloTabela("Vendedor", "Vendas", "Valor total");

    public PainelRelatorios() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        txtDe.setText(Fmt.data(LocalDate.now().withDayOfMonth(1)));
        txtAte.setText(Fmt.data(LocalDate.now()));

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(UI.linha(new JLabel("Período — de:"), txtDe, new JLabel("até:"), txtAte, UI.botao("Gerar", this::gerar)), BorderLayout.NORTH);
        lblResumo.setFont(lblResumo.getFont().deriveFont(Font.BOLD, 14f));
        lblResumo.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 0));
        norte.add(lblResumo, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        JPanel p1 = new JPanel(new BorderLayout());
        p1.setBorder(BorderFactory.createTitledBorder("Produtos mais vendidos (top 10)"));
        p1.add(new JScrollPane(UI.tabela(modeloProdutos)));
        JPanel p2 = new JPanel(new BorderLayout());
        p2.setBorder(BorderFactory.createTitledBorder("Vendas por vendedor"));
        p2.add(new JScrollPane(UI.tabela(modeloVendedores)));
        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 8));
        centro.add(p1);
        centro.add(p2);
        add(centro, BorderLayout.CENTER);
    }

    @Override public void atualizar() { UI.executar(this, this::gerar); }

    private void gerar() {
        LocalDate de = Fmt.lerData(txtDe.getText(), "De");
        LocalDate ate = Fmt.lerData(txtAte.getText(), "Até");
        RelatorioService.Resumo r = service.resumo(de, ate);
        lblResumo.setText("Vendas concluídas: " + r.vendas() + "   |   Valor total: " + Fmt.moeda(r.valor())
                + "   |   Canceladas no período: " + r.canceladas() + " (não contabilizadas)");
        modeloProdutos.setDados(service.maisVendidos(de, ate).stream().map(l -> new Object[]{l[0], l[1],
                ((Number) l[2]).intValue(), Fmt.moeda((BigDecimal) l[3])}).toList());
        modeloVendedores.setDados(service.porVendedor(de, ate).stream().map(l -> new Object[]{l[0],
                ((Number) l[1]).intValue(), Fmt.moeda((BigDecimal) l[2])}).toList());
    }
}
