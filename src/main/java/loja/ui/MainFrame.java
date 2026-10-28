package loja.ui;

import loja.model.Usuario;
import loja.service.AuthService;
import loja.service.Sessao;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Janela principal: barra lateral (menu por perfil) + área de conteúdo. */
public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel conteudo = new JPanel(cards);
    private final Map<String, Atualizavel> telas = new LinkedHashMap<>();

    public MainFrame() {
        super("Loja de Informática");
        Usuario u = Sessao.exigirLogado();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);

        JPanel lateral = new JPanel();
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setBackground(UI.PRIMARIA);
        lateral.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
        JLabel marca = new JLabel("Loja de Informática");
        marca.setForeground(Color.WHITE);
        marca.setFont(marca.getFont().deriveFont(Font.BOLD, 15f));
        lateral.add(marca);
        lateral.add(Box.createVerticalStrut(14));

        registrar(lateral, "Início", new PainelInicio());
        registrar(lateral, "Produtos", new PainelProdutos());
        if (u.isAdmin()) {
            registrar(lateral, "Categorias", new PainelCategorias());
            registrar(lateral, "Estoque", new PainelEstoque());
        }
        registrar(lateral, "Nova venda", new PainelVendas());
        registrar(lateral, u.isAdmin() ? "Histórico de vendas" : "Minhas vendas", new PainelHistorico());
        if (u.isAdmin()) {
            registrar(lateral, "Relatórios", new PainelRelatorios());
            registrar(lateral, "Usuários", new PainelUsuarios());
        }
        lateral.add(Box.createVerticalGlue());
        JButton sair = botaoMenu("Sair");
        sair.addActionListener(e -> sair());
        lateral.add(sair);

        setLayout(new BorderLayout());
        add(lateral, BorderLayout.WEST);
        add(conteudo, BorderLayout.CENTER);
        mostrar("Início");
    }

    private void registrar(JPanel lateral, String nome, JPanel painel) {
        telas.put(nome, (Atualizavel) painel);
        conteudo.add(painel, nome);
        JButton b = botaoMenu(nome);
        b.addActionListener(e -> mostrar(nome));
        lateral.add(b);
        lateral.add(Box.createVerticalStrut(6));
    }

    private JButton botaoMenu(String texto) {
        JButton b = new JButton(texto);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setFocusPainted(false);
        return b;
    }

    private void mostrar(String nome) {
        cards.show(conteudo, nome);
        telas.get(nome).atualizar();
    }

    private void sair() {
        if (!UI.confirmar(this, "Deseja encerrar a sessão?")) return;
        new AuthService().sair();
        dispose();
        new LoginFrame().setVisible(true);
    }
}
