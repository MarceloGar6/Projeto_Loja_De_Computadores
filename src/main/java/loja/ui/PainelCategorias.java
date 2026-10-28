package loja.ui;

import loja.model.Categoria;
import loja.service.CategoriaService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PainelCategorias extends JPanel implements Atualizavel {
    private final CategoriaService service = new CategoriaService();
    private final ModeloTabela modelo = new ModeloTabela("Nome", "Descrição", "Status");
    private final JTable tabela = UI.tabela(modelo);
    private List<Categoria> categorias = List.of();

    public PainelCategorias() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(UI.linha(UI.botao("Nova categoria", () -> salvar(null)),
                UI.botao("Editar", () -> salvar(categorias.get(UI.selecionada(tabela)))),
                UI.botao("Ativar / Desativar", this::alternar)), BorderLayout.SOUTH);
    }

    @Override
    public void atualizar() {
        UI.executar(this, () -> {
            categorias = service.listar(false);
            modelo.setDados(categorias.stream().map(c -> new Object[]{c.nome(), c.descricao(), c.ativo() ? "Ativa" : "Inativa"}).toList());
        });
    }

    private void salvar(Categoria c) {
        JTextField nome = new JTextField(c == null ? "" : c.nome(), 22);
        JTextField desc = new JTextField(c == null ? "" : c.descricao(), 22);
        Form f = new Form();
        f.campo("Nome *", nome);
        f.campo("Descrição", desc);
        if (JOptionPane.showConfirmDialog(this, f, c == null ? "Nova categoria" : "Editar categoria",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        service.salvar(c == null ? 0 : c.id(), nome.getText(), desc.getText());
        atualizar();
    }

    private void alternar() {
        Categoria c = categorias.get(UI.selecionada(tabela));
        service.alterarAtivo(c.id(), !c.ativo());
        atualizar();
    }
}
