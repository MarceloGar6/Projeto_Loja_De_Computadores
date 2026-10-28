package loja.service;

import loja.dao.UsuarioDAO;
import loja.model.TipoUsuario;
import loja.model.Usuario;
import loja.util.*;

public class AuthService {
    private final UsuarioDAO dao = new UsuarioDAO();

    /** Mensagem única para usuário inexistente ou senha errada: não revela qual dos dois falhou. */
    public Usuario entrar(String login, String senha) {
        String l = Validar.obrigatorio(login, "Usuário ou e-mail");
        if (senha == null || senha.isEmpty()) throw new NegocioException("Informe a senha.");
        UsuarioDAO.Credencial cred = Database.consultar(c -> dao.buscarPorLogin(c, l)).orElse(null);
        if (cred == null || !SenhaUtil.conferir(senha, cred.senhaHash()))
            throw new NegocioException("Usuário ou senha inválidos.");
        if (!cred.usuario().ativo())
            throw new NegocioException("Esta conta está desativada. Procure um administrador.");
        Sessao.iniciar(cred.usuario());
        return cred.usuario();
    }

    public void sair() { Sessao.encerrar(); }

    public boolean precisaCriarAdminInicial() {
        return Database.consultar(dao::contar) == 0;
    }

    /** Só funciona enquanto a tabela de usuários está vazia (implantação do sistema). */
    public void criarAdminInicial(String nomeCompleto, String nomeUsuario, String email, String senha) {
        if (!precisaCriarAdminInicial()) throw new NegocioException("O administrador inicial já foi criado.");
        String nome = Validar.obrigatorio(nomeCompleto, "Nome completo");
        String usuario = Validar.obrigatorio(nomeUsuario, "Nome de usuário");
        String mail = Validar.email(email);
        Validar.senha(senha);
        Database.consultar(c -> { dao.inserir(c, nome, usuario, mail, SenhaUtil.gerarHash(senha), TipoUsuario.ADMIN); return null; });
    }
}
