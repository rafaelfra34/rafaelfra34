package com.pressfran.cobranzas;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class MessageBuilder {
    private static final DecimalFormat MONEY;
    static {
        DecimalFormatSymbols s = new DecimalFormatSymbols(new Locale("es", "AR"));
        s.setGroupingSeparator('.'); s.setDecimalSeparator(',');
        MONEY = new DecimalFormat("#,##0", s);
    }

    public static long overdueDays(Client c) {
        if (c == null || c.dueDate == null || c.dueDate.isEmpty()) return 0;
        LocalDate due = LocalDate.parse(c.dueDate); LocalDate today = LocalDate.now();
        return Math.max(0, ChronoUnit.DAYS.between(due, today));
    }

    public static double interest(Client c) { return c.installment * (c.dailyInterest / 100.0) * overdueDays(c); }
    public static double amountDue(Client c) { return Math.min(c.balance, c.installment) + interest(c); }

    public static String build(Client c, String alias) {
        LocalDate today = LocalDate.now(); LocalDate due = LocalDate.parse(c.dueDate);
        long days = overdueDays(c); double interest = interest(c);
        StringBuilder m = new StringBuilder();
        m.append("Buen día, ").append(c.name).append(".\n\n");
        if (!c.business.isEmpty()) m.append(c.business).append("\n\n");
        if (!c.commitmentDate.isEmpty() && c.commitmentDate.equals(today.toString()))
            m.append("Para hoy tenemos registrado tu compromiso de pago con PressFran.\n\n");
        if (today.isBefore(due)) {
            m.append("Te recordamos que tu próxima cuota vence el ").append(formatDate(due)).append(".\n");
            m.append("Cuota: ").append(money(c.installment)).append("\n");
        } else if (today.equals(due)) {
            m.append("Te recordamos que hoy vence tu cuota.\n");
            m.append("Cuota: ").append(money(c.installment)).append("\n");
        } else {
            m.append("Tu cuota registra ").append(days).append(days == 1 ? " día" : " días").append(" de atraso.\n");
            m.append("Cuota: ").append(money(Math.min(c.balance, c.installment))).append("\n");
            m.append("Interés por mora (").append(trim(c.dailyInterest)).append("% diario): ").append(money(interest)).append("\n");
            m.append("Total actualizado: ").append(money(amountDue(c))).append("\n");
        }
        m.append("Saldo pendiente: ").append(money(c.balance)).append("\n");
        m.append("Plan: ").append(c.installmentCount).append(" cuotas ").append(c.frequency.toLowerCase()).append("s\n\n");
        if (alias != null && !alias.trim().isEmpty()) m.append("Alias para transferencia: *").append(alias.trim()).append("*\n\n");
        if (today.isAfter(due)) m.append("Indicános por favor la fecha en que vas a regularizar el pago para registrar tu compromiso.\n\n");
        else m.append("Una vez realizado el pago, envianos el comprobante.\n\n");
        m.append("*PressFran – Préstamos a Comerciantes*\n_Te ayudamos a crecer._");
        return m.toString();
    }

    public static String money(double value) { synchronized (MONEY) { return "$ " + MONEY.format(Math.round(value)); } }
    public static String formatDate(LocalDate d) { return String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear()); }
    private static String trim(double x) { return x == Math.rint(x) ? String.valueOf((int)x) : String.valueOf(x); }
}
