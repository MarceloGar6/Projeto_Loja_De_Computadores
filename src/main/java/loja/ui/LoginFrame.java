package loja.ui;

import loja.service.AuthService;
import loja.util.DaoException;
import loja.util.NegocioException;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final AuthService auth = new AuthService();
    private final JTextField txtUsuario = new JTextField(20);
    private final JPasswordField txtSenha = new JPasswordField(20);
    private final JLabel lblErro = new JLabel(" ");

    public LoginFrame() {
        super("Loja de Informática — Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel titulo = new JLabel("Loja de Informática", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        titulo.setForeground(UI.PRIMARIA);
        lblErro.setForeground(Color.RED);
        lblErro.setHorizontalAlignment(SwingConstants.CENTER);

        Form form = new Form();
        form.campo("Usuário ou e-mail:", txtUsuario);
        form.campo("Senha:", txtSenha);

        JButton entrar = new JButton("Entrar");
        entrar.addActionListener(e -> entrar());
        getRootPane().setDefaultButton(entrar);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        centro.add(titulo, BorderLayout.NORTH);
        JPanel meio = new JPanel(new BorderLayout());
        meio.add(form, BorderLayout.CENTER);
        meio.add(lblErro, BorderLayout.SOUTH);
        centro.add(meio, BorderLayout.CENTER);
        JPanel botoes = new JPanel();
        botoes.add(entrar);
        centro.add(botoes, BorderLayout.SOUTH);

        setContentPane(centro);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void entrar() {
        lblErro.setText(" ");
        try {
            auth.entrar(txtUsuario.getText(), new String(txtSenha.getPassword()));
            txtSenha.setText("");
            dispose();
            new MainFrame().setVisible(true);
        } catch (NegocioException | DaoException e) {
            lblErro.setText(e.getMessage());
            txtSenha.setText("");
        }
    }
}
