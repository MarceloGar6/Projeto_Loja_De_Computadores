package loja.service;

import loja.model.Usuario;
import loja.util.NegocioException;

/** Guarda o usuário logado e centraliza as verificações de permissão (RN01, RN02, RN18). */
public final class Sessao {
    private static Usuario atual;

    private Sessao() { }

    public static void iniciar(Usuario u) { atual = u; }
    public static void encerrar() { atual = null; }
    public static Usuario atual() { return atual; }

    public static Usuario exigirLogado() {
        if (atual == null) throw new NegocioException("Sessão encerrada. Faça login novamente.");
        return atual;
    }

    /** Chamado no início de toda operação administrativa — a interface gráfica sozinha não basta. */
    public static Usuario exigirAdmin() {
        Usuario u = exigirLogado();
        if (!u.isAdmin()) throw new NegocioException("Você não tem permissão para executar esta operação.");
        return u;
    }
}
