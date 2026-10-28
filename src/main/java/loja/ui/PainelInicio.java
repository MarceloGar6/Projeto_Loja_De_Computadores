package loja.ui;

import loja.model.Usuario;
import loja.service.RelatorioService;
import loja.service.Sessao;
import loja.util.Fmt;

import javax.swing.*;
import java.awt.*;

/** Painel principal: indicadores diferentes para administrador e vendedor. */
public class PainelInicio extends JPanel implements Atualizavel {
    private final RelatorioService service = new RelatorioService();
    private final JPanel cards = new JPanel(new GridLayout(0, 3, 12, 12));
    private final JLabel boasVindas = new JLabel();

    public PainelInicio() {
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        boasVindas.setFont(boasVindas.getFont().deriveFont(Font.BOLD, 20f));
        add(boasVindas, BorderLayout.NORTH);
        JPanel topo = new JPanel(new BorderLayout());
        topo.add(cards, BorderLayout.NORTH);
        add(topo, BorderLayout.CENTER);
    }

    @Override
    public void atualizar() {
        UI.executar(this, () -> {
            Usuario u = Sessao.exigirLogado();
            RelatorioService.Painel p = service.painel();
            boasVindas.setText("Olá, " + u.nomeCompleto() + " (" + u.tipo() + ")");
            cards.removeAll();
            if (u.isAdmin()) {
                card("Produtos cadastrados", String.valueOf(p.cadastrados()), null);
                card("Produtos disponíveis", String.valueOf(p.disponiveis()), null);
                card("Estoque baixo", String.valueOf(p.baixo()), p.baixo() > 0 ? UI.ALERTA : null);
                card("Sem estoque", String.valueOf(p.semEstoque()), p.semEstoque() > 0 ? UI.PERIGO : null);
                card("Vendas hoje", String.valueOf(p.vendasHoje()), null);
                card("Total vendido hoje", Fmt.moeda(p.valorHoje()), null);
            } else {
                card("Minhas vendas hoje", String.valueOf(p.vendasHoje()), null);
                card("Total vendido hoje", Fmt.moeda(p.valorHoje()), null);
            }
            cards.revalidate();
            cards.repaint();
        });
    }

    private void card(String titulo, String valor, Color fundo) {
        JPanel c = new JPanel(new BorderLayout());
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        if (fundo != null) c.setBackground(fundo);
        JLabel t = new JLabel(titulo);
        JLabel v = new JLabel(valor);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 26f));
        c.add(t, BorderLayout.NORTH);
        c.add(v, BorderLayout.CENTER);
        cards.add(c);
    }
}
