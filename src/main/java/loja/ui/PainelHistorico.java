package loja.ui;

import loja.model.SituacaoVenda;
import loja.model.Usuario;
import loja.model.Venda;
import loja.service.Sessao;
import loja.service.UsuarioService;
import loja.service.VendaService;
import loja.util.Fmt;
import loja.util.NegocioException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;

/** Histórico de vendas. Administrador vê todas e pode cancelar; vendedor vê só as suas. */
public class PainelHistorico extends JPanel implements Atualizavel {
    private final VendaService service = new VendaService();
    private final UsuarioService usuarioService = new UsuarioService();
    private final boolean admin = Sessao.exigirLogado().isAdmin();

    private final JTextField txtNumero = new JTextField(6), txtDe = new JTextField(9), txtAte = new JTextField(9);
    private final JComboBox<Object> cmbVendedor = new JComboBox<>();
    private final JComboBox<Object> cmbSituacao = new JComboBox<>();
    private final ModeloTabela modelo = new ModeloTabela("Nº", "Data/hora", "Vendedor", "Total", "Pagamento", "Situação");
    private final JTable tabela = UI.tabela(modelo);
    private List<Venda> vendas = List.of();

    public PainelHistorico() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        cmbSituacao.addItem("Todas");
        for (SituacaoVenda s : SituacaoVenda.values()) cmbSituacao.addItem(s);

        JPanel filtros = UI.linha(new JLabel("Nº:"), txtNumero, new JLabel("De:"), txtDe, new JLabel("Até:"), txtAte);
        if (admin) { filtros.add(new JLabel("Vendedor:")); filtros.add(cmbVendedor); }
        filtros.add(new JLabel("Situação:"));
        filtros.add(cmbSituacao);
        filtros.add(UI.botao("Pesquisar", this::carregar));
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(filtros, BorderLayout.CENTER);
        norte.add(new JLabel("  Datas no formato dd/mm/aaaa. Dê duplo clique numa venda para ver os detalhes."), BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel botoes = UI.linha(UI.botao("Ver detalhes", this::detalhes));
        if (admin) botoes.add(UI.botao("Cancelar venda", this::cancelar));
        add(botoes, BorderLayout.SOUTH);

        tabela.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) UI.executar(PainelHistorico.this, PainelHistorico.this::detalhes);
            }
        });
    }

    @Override
    public void atualizar() {
        UI.executar(this, () -> {
            if (admin) {
                Object sel = cmbVendedor.getSelectedItem();
                cmbVendedor.removeAllItems();
                cmbVendedor.addItem("Todos");
                for (Usuario u : usuarioService.listar()) cmbVendedor.addItem(u);
                if (sel != null) cmbVendedor.setSelectedItem(sel);
            }
            carregar();
        });
    }

    private void carregar() {
        Integer numero = txtNumero.getText().isBlank() ? null : Fmt.lerInteiro(txtNumero.getText(), "Nº da venda");
        LocalDate de = Fmt.lerData(txtDe.getText(), "De");
        LocalDate ate = Fmt.lerData(txtAte.getText(), "Até");
        Integer vendedor = admin && cmbVendedor.getSelectedItem() instanceof Usuario u ? u.id() : null;
        SituacaoVenda sit = cmbSituacao.getSelectedItem() instanceof SituacaoVenda s ? s : null;
        vendas = service.pesquisar(numero, de, ate, vendedor, sit);
        modelo.setDados(vendas.stream().map(v -> new Object[]{v.id(), Fmt.dataHora(v.dataHora()), v.vendedorNome(),
                Fmt.moeda(v.total()), v.forma(), v.situacao()}).toList());
    }

    private void detalhes() {
        Venda v = service.detalhes(vendas.get(UI.selecionada(tabela)).id());
        Comprovante.mostrar(this, v);
    }

    private void cancelar() {
        Venda v = vendas.get(UI.selecionada(tabela));
        if (v.situacao() == SituacaoVenda.CANCELADA) throw new NegocioException("Esta venda já está cancelada.");
        Venda completa = service.detalhes(v.id());
        if (!UI.confirmarTexto(this, "Cancelar venda", Comprovante.texto(completa)
                + "\nO cancelamento devolve os produtos ao estoque e NÃO estorna o pagamento (tratar manualmente).\n"
                + "Confirmar o cancelamento?")) return;
        service.cancelar(v.id());
        UI.info(this, "Venda cancelada e estoque restaurado.");
        carregar();
    }
}
