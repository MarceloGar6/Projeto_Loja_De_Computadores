package loja.util;

import java.util.regex.Pattern;

/** Validações simples de entrada, usadas pela camada de serviço. */
public final class Validar {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Validar() { }

    public static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) throw new NegocioException("O campo \"" + campo + "\" é obrigatório.");
        return valor.trim();
    }

    public static String opcional(String valor) { return valor == null ? "" : valor.trim(); }

    public static String email(String valor) {
        String v = obrigatorio(valor, "E-mail");
        if (!EMAIL.matcher(v).matches()) throw new NegocioException("Informe um e-mail válido.");
        return v;
    }

    public static void senha(String senha) {
        if (senha == null || senha.length() < 6) throw new NegocioException("A senha deve ter pelo menos 6 caracteres.");
    }
}
