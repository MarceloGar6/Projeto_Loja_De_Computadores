package loja.ui;

import loja.model.Categoria;
import loja.model.Produto;
import loja.util.Fmt;
import loja.util.NegocioException;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.List;

/** Formulário de cadastro/edição de produto. */
public final class ProdutoDialog {
    private ProdutoDialog() { }

    /** @param existente null para novo produto. @return produto preenchido (ou null se cancelado). */
    public static Produto exibir(java.awt.Component pai, Produto existente, List<Categoria> categorias) {
        boolean novo = existente == null;
        JTextField codigo = new JTextField(existente == null ? "" : existente.codigo(), 22);
        JTextField nome = new JTextField(existente == null ? "" : existente.nome(), 22);
        JTextField descricao = new JTextField(existente == null ? "" : existente.descricao(), 22);
        JTextField marca = new JTextField(existente == null ? "" : existente.marca(), 22);
        JTextField modelo = new JTextField(existente == null ? "" : existente.modelo(), 22);
        JTextField preco = new JTextField(existente == null ? "" : existente.preco().toPlainString().replace('.', ','), 22);
        JTextField qtd = new JTextField(novo ? "0" : String.valueOf(existente.quantidade()), 22);
        qtd.setEnabled(novo);   // quantidade só muda pelo módulo de estoque depois do cadastro
        JTextField minimo = new JTextField(existente == null ? "0" : String.valueOf(existente.estoqueMinimo()), 22);
        JComboBox<Categoria> cmbCat = new JComboBox<>(categorias.toArray(new Categoria[0]));
        if (existente != null)
            for (Categoria c : categorias) if (c.id() == existente.categoriaId()) cmbCat.setSelectedItem(c);

        Form f = new Form();
        f.campo("Código *", codigo);
        f.campo("Nome *", nome);
        f.campo("Descrição", descricao);
        f.campo("Categoria *", cmbCat);
        f.campo("Marca", marca);
        f.campo("Modelo", modelo);
        f.campo("Preço de venda (R$) *", preco);
        f.campo(novo ? "Quantidade inicial" : "Quantidade (use Estoque)", qtd);
        f.campo("Estoque mínimo", minimo);

        int r = JOptionPane.showConfirmDialog(pai, f, novo ? "Novo produto" : "Editar produto",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return null;

        Categoria cat = (Categoria) cmbCat.getSelectedItem();
        if (cat == null) throw new NegocioException("Cadastre uma categoria antes de criar produtos.");
        BigDecimal p = Fmt.lerDinheiro(preco.getText(), "Preço de venda");
        int q = novo ? Fmt.lerInteiro(qtd.getText(), "Quantidade") : existente.quantidade();
        int m = Fmt.lerInteiro(minimo.getText(), "Estoque mínimo");
        return new Produto(novo ? 0 : existente.id(), codigo.getText(), nome.getText(), descricao.getText(),
                marca.getText(), modelo.getText(), cat.id(), cat.nome(), p, q, m, novo || existente.ativo());
    }
}
