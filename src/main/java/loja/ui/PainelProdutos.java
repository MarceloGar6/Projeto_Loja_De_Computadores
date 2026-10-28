package loja.ui;

import loja.model.Categoria;
import loja.model.Produto;
import loja.service.CategoriaService;
import loja.service.ProdutoService;
import loja.service.Sessao;
import loja.util.Fmt;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Consulta de produtos (todos) e gerenciamento (somente administrador). */
public class PainelProdutos extends JPanel implements Atualizavel {
    private final ProdutoService service = new ProdutoService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final boolean admin = Sessao.exigirLogado().isAdmin();

    private final JTextField txtBusca = new JTextField(16);
    private final JComboBox<Object> cmbCategoria = new JComboBox<>();
    private final JComboBox<String> cmbSituacao = new JComboBox<>(new String[]{"Todas", "Disponível", "Estoque baixo", "Sem estoque"});
    private final ModeloTabela modelo = new ModeloTabela("Código", "Nome", "Categoria", "Marca", "Modelo", "Preço", "Estoque", "Situação", "Status");
    private final JTable tabela = UI.tabela(modelo);
    private List<Produto> produtos = List.of();
    private boolean carregandoFiltros;

    public PainelProdutos() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        UI.destacarSituacao(tabela, 7);

        JButton buscar = UI.botao("Pesquisar", this::carregar);
        txtBusca.addActionListener(e -> UI.executar(this, this::carregar));
        cmbCategoria.addActionListener(e -> { if (!carregandoFiltros) UI.executar(this, this::carregar); });
        cmbSituacao.addActionListener(e -> { if (!carregandoFiltros) UI.executar(this, this::carregar); });
        add(UI.linha(new JLabel("Nome, código ou categoria:"), txtBusca, new JLabel("Categoria:"), cmbCategoria,
                new JLabel("Situação:"), cmbSituacao, buscar), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        if (admin) {
            JPanel botoes = UI.linha(
                    UI.botao("Novo produto", this::novo),
                    UI.botao("Editar", this::editar),
                    UI.botao("Excluir / Desativar", this::excluir),
                    UI.botao("Reativar", this::reativar));
            add(botoes, BorderLayout.SOUTH);
        }
    }

    @Override
    public void atualizar() {
        UI.executar(this, () -> {
            carregandoFiltros = true;
            Object selecionada = cmbCategoria.getSelectedItem();
            cmbCategoria.removeAllItems();
            cmbCategoria.addItem("Todas");
            for (Categoria c : categoriaService.listar(false)) cmbCategoria.addItem(c);
            if (selecionada != null) cmbCategoria.setSelectedItem(selecionada);
            carregandoFiltros = false;
            carregar();
        });
    }

    private void carregar() {
        Integer catId = cmbCategoria.getSelectedItem() instanceof Categoria c ? c.id() : null;
        String sit = "Todas".equals(cmbSituacao.getSelectedItem()) ? null : (String) cmbSituacao.getSelectedItem();
        produtos = service.pesquisar(txtBusca.getText(), catId, sit);
        modelo.setDados(produtos.stream().map(p -> new Object[]{p.codigo(), p.nome(), p.categoriaNome(), p.marca(), p.modelo(),
                Fmt.moeda(p.preco()), p.quantidade(), p.situacaoEstoque(), p.ativo() ? "Ativo" : "Inativo"}).toList());
    }

    private void novo() {
        Produto p = ProdutoDialog.exibir(this, null, categoriaService.listar(true));
        if (p == null) return;
        service.cadastrar(p);
        UI.info(this, "Produto cadastrado com sucesso.");
        carregar();
    }

    private void editar() {
        Produto atual = produtos.get(UI.selecionada(tabela));
        Produto p = ProdutoDialog.exibir(this, atual, categoriaService.listar(true));
        if (p == null) return;
        service.editar(p);
        UI.info(this, "Produto atualizado com sucesso.");
        carregar();
    }

    private void excluir() {
        Produto p = produtos.get(UI.selecionada(tabela));
        if (!UI.confirmar(this, "Excluir/desativar o produto \"" + p.nome() + "\"?\n"
                + "Se ele já tiver histórico, será apenas desativado.")) return;
        UI.info(this, service.excluirOuDesativar(p.id()));
        carregar();
    }

    private void reativar() {
        Produto p = produtos.get(UI.selecionada(tabela));
        service.reativar(p.id());
        carregar();
    }
}
