package loja.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Formatação e leitura de valores (moeda, datas) no padrão brasileiro. */
public final class Fmt {
    private static final Locale BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Fmt() { }

    public static String moeda(BigDecimal v) { return NumberFormat.getCurrencyInstance(BR).format(v == null ? BigDecimal.ZERO : v); }
    public static String dataHora(LocalDateTime d) { return d == null ? "" : d.format(DATA_HORA); }
    public static String data(LocalDate d) { return d == null ? "" : d.format(DATA); }

    /** Lê "1234,56" ou "1234.56" como valor monetário (BigDecimal, nunca double). */
    public static BigDecimal lerDinheiro(String texto, String campo) {
        try {
            String t = texto.trim().replace("R$", "").replace(" ", "");
            if (t.contains(",")) t = t.replace(".", "").replace(",", ".");
            return new BigDecimal(t).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (RuntimeException e) {
            throw new NegocioException("Valor inválido no campo \"" + campo + "\". Exemplo: 199,90");
        }
    }

    public static int lerInteiro(String texto, String campo) {
        try { return Integer.parseInt(texto.trim()); }
        catch (RuntimeException e) { throw new NegocioException("Valor inválido no campo \"" + campo + "\". Informe um número inteiro."); }
    }

    /** Data opcional no formato dd/MM/aaaa; vazio retorna null. */
    public static LocalDate lerData(String texto, String campo) {
        if (texto == null || texto.isBlank()) return null;
        try { return LocalDate.parse(texto.trim(), DATA); }
        catch (DateTimeParseException e) { throw new NegocioException("Data inválida em \"" + campo + "\". Use dd/mm/aaaa."); }
    }
}
