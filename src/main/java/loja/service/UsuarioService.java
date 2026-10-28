package loja.service;

import loja.dao.UsuarioDAO;
import loja.model.TipoUsuario;
import loja.model.Usuario;
import loja.util.*;

import java.util.List;

/** Gerenciamento de contas — exclusivo do administrador. */
public class UsuarioService {
    private final UsuarioDAO dao = new UsuarioDAO();

    public List<Usuario> listar() {
        Sessao.exigirAdmin();
        return Database.consultar(dao::listar);
    }

    public void cadastrar(String nomeCompleto, String nomeUsuario, String email, String senha, TipoUsuario tipo) {
        Sessao.exigirAdmin();
        String nome = Validar.obrigatorio(nomeCompleto, "Nome completo");
        String usuario = Validar.obrigatorio(nomeUsuario, "Nome de usuário");
        String mail = Validar.email(email);
        Validar.senha(senha);
        if (tipo == null) throw new NegocioException("Selecione o tipo de usuário.");
        try {
            Database.consultar(c -> {
                if (dao.existe(c, usuario, mail, 0)) throw new NegocioException("Já existe uma conta com este usuário ou e-mail.");
                dao.inserir(c, nome, usuario, mail, SenhaUtil.gerarHash(senha), tipo);
                return null;
            });
        } catch (DaoException e) {
            if (e.isDuplicado()) throw new NegocioException("Já existe uma conta com este usuário ou e-mail.");
            throw e;
        }
    }

    public void editar(int id, String nomeCompleto, String nomeUsuario, String email, TipoUsuario tipo) {
        Usuario logado = Sessao.exigirAdmin();
        String nome = Validar.obrigatorio(nomeCompleto, "Nome completo");
        String usuario = Validar.obrigatorio(nomeUsuario, "Nome de usuário");
        String mail = Validar.email(email);
        if (tipo == null) throw new NegocioException("Selecione o tipo de usuário.");
        try {
            Database.transacao(c -> {
                Usuario atual = dao.buscarPorId(c, id).orElseThrow(() -> new NegocioException("Usuário não encontrado."));
                if (dao.existe(c, usuario, mail, id)) throw new NegocioException("Já existe outra conta com este usuário ou e-mail.");
                // Não permite rebaixar o último administrador ativo.
                if (atual.isAdmin() && atual.ativo() && tipo != TipoUsuario.ADMIN && dao.contarAdminsAtivos(c) <= 1)
                    throw new NegocioException("Não é possível remover a permissão do último administrador ativo.");
                dao.atualizar(c, id, nome, usuario, mail, tipo);
                return null;
            });
        } catch (DaoException e) {
            if (e.isDuplicado()) throw new NegocioException("Já existe outra conta com este usuário ou e-mail.");
            throw e;
        }
        if (logado.id() == id) { // mantém a sessão coerente se o admin editar a si mesmo
            Database.consultar(c -> dao.buscarPorId(c, id)).ifPresent(Sessao::iniciar);
        }
    }

    public void redefinirSenha(int id, String novaSenha) {
        Sessao.exigirAdmin();
        Validar.senha(novaSenha);
        Database.consultar(c -> { dao.atualizarSenha(c, id, SenhaUtil.gerarHash(novaSenha)); return null; });
    }

    public void alterarAtivo(int id, boolean ativo) {
        Usuario logado = Sessao.exigirAdmin();
        if (!ativo && logado.id() == id) throw new NegocioException("Você não pode desativar a sua própria conta.");
        Database.transacao(c -> {
            Usuario alvo = dao.buscarPorId(c, id).orElseThrow(() -> new NegocioException("Usuário não encontrado."));
            if (!ativo && alvo.isAdmin() && alvo.ativo() && dao.contarAdminsAtivos(c) <= 1)
                throw new NegocioException("Não é possível desativar o último administrador ativo.");
            dao.alterarAtivo(c, id, ativo);
            return null;
        });
    }
}
