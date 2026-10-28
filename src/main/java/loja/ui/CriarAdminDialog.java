package loja.ui;

import loja.service.AuthService;

import javax.swing.*;
import java.awt.*;

/** Exibido apenas na implantação, quando ainda não existe nenhum usuário. */
public class CriarAdminDialog {
    /** @return true se o administrador foi criado. */
    public static boolean exibir(AuthService auth) {
        JTextField nome = new JTextField(22), usuario = new JTextField(22), email = new JTextField(22);
        JPasswordField senha = new JPasswordField(22), confirma = new JPasswordField(22);
        Form f = new Form();
        f.campo("Nome completo:", nome);
        f.campo("Nome de usuário:", usuario);
        f.campo("E-mail:", email);
        f.campo("Senha (mín. 6):", senha);
        f.campo("Confirmar senha:", confirma);
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.add(new JLabel("<html><b>Primeira execução:</b> crie a conta do administrador da loja.</html>"), BorderLayout.NORTH);
        p.add(f, BorderLayout.CENTER);

        while (true) {
            int r = JOptionPane.showConfirmDialog(null, p, "Implantação — administrador inicial",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return false;
            if (!new String(senha.getPassword()).equals(new String(confirma.getPassword()))) {
                UI.erro(null, "As senhas não coincidem.");
                continue;
            }
            try {
                auth.criarAdminInicial(nome.getText(), usuario.getText(), email.getText(), new String(senha.getPassword()));
                UI.info(null, "Administrador criado com sucesso. Faça login para continuar.");
                return true;
            } catch (RuntimeException e) {
                UI.erro(null, e.getMessage());
            }
        }
    }
}
