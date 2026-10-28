package loja.service;

import loja.dao.CategoriaDAO;
import loja.model.Categoria;
import loja.util.*;

import java.util.List;

public class CategoriaService {
    private final CategoriaDAO dao = new CategoriaDAO();

    public List<Categoria> listar(boolean apenasAtivas) {
        Sessao.exigirLogado();
        return Database.consultar(c -> dao.listar(c, apenasAtivas));
    }

    public void salvar(int id, String nome, String descricao) {
        Sessao.exigirAdmin();
        String n = Validar.obrigatorio(nome, "Nome da categoria");
        String d = Validar.opcional(descricao);
        try {
            Database.consultar(c -> {
                if (dao.existeNome(c, n, id)) throw new NegocioException("Já existe uma categoria com este nome.");
                if (id == 0) dao.inserir(c, n, d); else dao.atualizar(c, id, n, d);
                return null;
            });
        } catch (DaoException e) {
            if (e.isDuplicado()) throw new NegocioException("Já existe uma categoria com este nome.");
            throw e;
        }
    }

    public void alterarAtivo(int id, boolean ativo) {
        Sessao.exigirAdmin();
        Database.consultar(c -> { dao.alterarAtivo(c, id, ativo); return null; });
    }
}
