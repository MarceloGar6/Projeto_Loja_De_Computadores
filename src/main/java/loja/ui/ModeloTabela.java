package loja.ui;

import javax.swing.table.DefaultTableModel;
import java.util.List;

/** Modelo de tabela somente leitura, preenchido a partir de listas de linhas. */
public class ModeloTabela extends DefaultTableModel {
    public ModeloTabela(String... colunas) { super(colunas, 0); }
    @Override public boolean isCellEditable(int linha, int coluna) { return false; }
    public void setDados(List<Object[]> linhas) {
        setRowCount(0);
        for (Object[] l : linhas) addRow(l);
    }
}
