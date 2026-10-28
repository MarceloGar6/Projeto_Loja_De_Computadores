package loja;

import loja.service.AuthService;
import loja.ui.CriarAdminDialog;
import loja.ui.LoginFrame;
import loja.ui.UI;

import javax.swing.*;

/** Ponto de entrada do sistema. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignorado) { /* usa o visual padrão do Java */ }

            AuthService auth = new AuthService();
            try {
                if (auth.precisaCriarAdminInicial() && !CriarAdminDialog.exibir(auth)) System.exit(0);
            } catch (RuntimeException e) {
                UI.erro(null, "Não foi possível conectar ao banco de dados.\n"
                        + "Verifique se o MySQL está ligado, se o script sql/schema.sql foi executado\n"
                        + "e se usuário/senha em src/main/resources/db.properties estão corretos.");
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }
}
