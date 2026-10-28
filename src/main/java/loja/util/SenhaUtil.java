package loja.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Hash de senha com PBKDF2-HMAC-SHA256 + salt aleatório (já incluso no Java, sem bibliotecas externas). */
public final class SenhaUtil {
    private static final int ITERACOES = 120_000;
    private static final int TAMANHO_BITS = 256;

    private SenhaUtil() { }

    public static String gerarHash(String senha) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(senha.toCharArray(), salt, ITERACOES);
        return "pbkdf2$" + ITERACOES + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean conferir(String senha, String armazenado) {
        try {
            String[] p = armazenado.split("\\$");
            int it = Integer.parseInt(p[1]);
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] esperado = Base64.getDecoder().decode(p[3]);
            return MessageDigest.isEqual(esperado, pbkdf2(senha.toCharArray(), salt, it));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] senha, byte[] salt, int iteracoes) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(senha, salt, iteracoes, TAMANHO_BITS)).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Algoritmo de hash indisponível", e);
        }
    }
}
