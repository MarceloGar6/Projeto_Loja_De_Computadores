package loja.ui;

import loja.model.ItemVenda;
import loja.model.Venda;
import loja.util.Fmt;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterException;

/** Texto e janela do comprovante / detalhes de uma venda (sem valor fiscal). */
public final class Comprovante {
    private Comprovante() { }

    public static String texto(Venda v) {
        StringBuilder sb = new StringBuilder();
        sb.append("========== COMPROVANTE DE VENDA ==========\n");
        sb.append("Venda nº:   ").append(v.id()).append('\n');
        sb.append("Data/hora:  ").append(Fmt.dataHora(v.dataHora())).append('\n');
        sb.append("Vendedor:   ").append(v.vendedorNome()).append('\n');
        sb.append("Pagamento:  ").append(v.forma()).append('\n');
        sb.append("Situação:   ").append(v.situacao()).append('\n');
        if (v.canceladaEm() != null)
            sb.append("Cancelada:  ").append(Fmt.dataHora(v.canceladaEm())).append(" por ").append(v.canceladaPor()).append('\n');
        sb.append("------------------------------------------\n");
        sb.append(String.format("%-26s %4s %10s %11s%n", "Produto", "Qtd", "Unit.", "Subtotal"));
        for (ItemVenda i : v.itens()) {
            String nome = i.nomeProduto().length() > 26 ? i.nomeProduto().substring(0, 25) + "…" : i.nomeProduto();
            sb.append(String.format("%-26s %4d %10s %11s%n", nome, i.quantidade(), Fmt.moeda(i.precoUnitario()), Fmt.moeda(i.subtotal())));
        }
        sb.append("------------------------------------------\n");
        sb.append(String.format("TOTAL: %35s%n", Fmt.moeda(v.total())));
        sb.append("Documento sem valor fiscal.\n");
        return sb.toString();
    }

    public static void mostrar(Component pai, Venda v) {
        JTextArea area = new JTextArea(texto(v), 20, 46);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        Object[] opcoes = {"Imprimir", "Fechar"};
        int r = JOptionPane.showOptionDialog(pai, new JScrollPane(area), "Venda nº " + v.id(),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opcoes, opcoes[1]);
        if (r == 0) {
            try { area.print(); }
            catch (PrinterException e) { UI.erro(pai, "Não foi possível imprimir o comprovante."); }
        }
    }
}
