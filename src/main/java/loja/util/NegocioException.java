package loja.util;

/** Erro de regra de negócio: a mensagem é segura e pensada para ser exibida ao usuário. */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensagem) { super(mensagem); }
}
