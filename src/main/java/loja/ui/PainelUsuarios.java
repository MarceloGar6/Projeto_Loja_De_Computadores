package loja.ui;

import loja.model.TipoUsuario;
import loja.model.Usuario;
import loja.service.UsuarioService;
import loja.util.Fmt;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PainelUsuarios extends JPanel implements Atualizavel {
    private final UsuarioService service = new UsuarioService();
    private final ModeloTabela modelo = new ModeloTabela("Nome", "Usuário", "E-mail", "Perfil", "Status", "Criado em");
    private final JTable tabela = UI.tabela(modelo);
    private List<Usuario> usuarios = List.of();

    public PainelUsuarios() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(UI.linha(UI.botao("Novo usuário", () -> formulario(null)),
                UI.botao("Editar", () -> formulario(usuarios.get(UI.selecionada(tabela)))),
                UI.botao("Redefinir senha", this::redefinirSenha),
                UI.botao("Ativar / Desativar", this::alternar)), BorderLayout.SOUTH);
    }

    @Override
    public void atualizar() {
        UI.executar(this, () -> {
            usuarios = service.listar();
            modelo.setDados(usuarios.stream().map(u -> new Object[]{u.nomeCompleto(), u.nomeUsuario(), u.email(), u.tipo(),
                    u.ativo() ? "Ativo" : "Desativado", Fmt.dataHora(u.criadoEm())}).toList());
        });
    }

    private void formulario(Usuario u) {
        JTextField nome = new JTextField(u == null ? "" : u.nomeCompleto(), 22);
        JTextField login = new JTextField(u == null ? "" : u.nomeUsuario(), 22);
        JTextField email = new JTextField(u == null ? "" : u.email(), 22);
        JComboBox<TipoUsuario> tipo = new JComboBox<>(TipoUsuario.values());
        tipo.setSelectedItem(u == null ? TipoUsuario.VENDEDOR : u.tipo());
        JPasswordField senha = new JPasswordField(22);
        Form f = new Form();
        f.campo("Nome completo *", nome);
        f.campo("Nome de usuário *", login);
        f.campo("E-mail *", email);
        f.campo("Perfil *", tipo);
        if (u == null) f.campo("Senha (mín. 6) *", senha);
        if (JOptionPane.showConfirmDialog(this, f, u == null ? "Novo usuário" : "Editar usuário",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        if (u == null) service.cadastrar(nome.getText(), login.getText(), email.getText(),
                new String(senha.getPassword()), (TipoUsuario) tipo.getSelectedItem());
        else service.editar(u.id(), nome.getText(), login.getText(), email.getText(), (TipoUsuario) tipo.getSelectedItem());
        atualizar();
    }

    private void redefinirSenha() {
        Usuario u = usuarios.get(UI.selecionada(tabela));
        JPasswordField s1 = new JPasswordField(20), s2 = new JPasswordField(20);
        Form f = new Form();
        f.campo("Nova senha:", s1);
        f.campo("Confirmar:", s2);
        if (JOptionPane.showConfirmDialog(this, f, "Redefinir senha de " + u.nomeUsuario(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        if (!new String(s1.getPassword()).equals(new String(s2.getPassword())))
            throw new loja.util.NegocioException("As senhas não coincidem.");
        service.redefinirSenha(u.id(), new String(s1.getPassword()));
        UI.info(this, "Senha redefinida.");
    }

    private void alternar() {
        Usuario u = usuarios.get(UI.selecionada(tabela));
        if (!UI.confirmar(this, (u.ativo() ? "Desativar" : "Reativar") + " a conta de " + u.nomeCompleto() + "?")) return;
        service.alterarAtivo(u.id(), !u.ativo());
        atualizar();
    }
}
