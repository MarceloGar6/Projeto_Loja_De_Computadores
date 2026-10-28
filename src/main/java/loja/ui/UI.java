package loja.ui;

import loja.util.DaoException;
import loja.util.NegocioException;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Utilitários de interface: mensagens, botões seguros e tabelas. */
public final class UI {
    public static final Color PRIMARIA = new Color(0x1F3A5F);
    public static final Color ALERTA = new Color(0xFFF3CD);
    public static final Color PERIGO = new Color(0xF8D7DA);

    private UI() { }

    public static void info(Component pai, String msg) {
        JOptionPane.showMessageDialog(pai, msg, "Informação", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void erro(Component pai, String msg) {
        JOptionPane.showMessageDialog(pai, msg, "Atenção", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirmar(Component pai, String msg) {
        return JOptionPane.showConfirmDialog(pai, msg, "Confirmação", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Confirmação com texto longo (resumo de venda etc.). */
    public static boolean confirmarTexto(Component pai, String titulo, String texto) {
        JTextArea area = new JTextArea(texto, 16, 52);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return JOptionPane.showConfirmDialog(pai, new JScrollPane(area), titulo, JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Executa uma ação convertendo qualquer erro em mensagem amigável (sem detalhes técnicos). */
    public static void executar(Component pai, Runnable acao) {
        try {
            acao.run();
        } catch (NegocioException | DaoException e) {
            erro(pai, e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            erro(pai, "Ocorreu um erro inesperado. Tente novamente ou procure o administrador.");
        }
    }

    public static JButton botao(String texto, Runnable acao) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> executar(b, acao));
        return b;
    }

    public static JTable tabela(ModeloTabela modelo) {
        JTable t = new JTable(modelo);
        t.setRowHeight(24);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    /** Pinta linhas de "Estoque baixo" (amarelo) e "Sem estoque" (vermelho) — alerta visual do RF19. */
    public static void destacarSituacao(JTable t, int colunaSituacao) {
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tb, v, sel, foc, r, c);
                if (!sel) {
                    String s = String.valueOf(tb.getValueAt(r, colunaSituacao));
                    comp.setBackground("Sem estoque".equals(s) ? PERIGO : "Estoque baixo".equals(s) ? ALERTA : Color.WHITE);
                }
                return comp;
            }
        });
    }

    public static JPanel linha(Component... itens) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        for (Component c : itens) p.add(c);
        return p;
    }

    /** Linha selecionada convertida para o índice do modelo (necessário por causa da ordenação). */
    public static int selecionada(JTable t) {
        int v = t.getSelectedRow();
        if (v < 0) throw new NegocioException("Selecione um item na tabela.");
        return t.convertRowIndexToModel(v);
    }
}
