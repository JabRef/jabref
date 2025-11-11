package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.strings.StringUtil;

public class Iso690FormatDate implements LayoutFormatter {

    @Override
    public String format(String s) {
        if (StringUtil.isBlank(s)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String[] date = s.split("de");
        // parte el string en los distintos campos de la fecha
        if (date.length == 1) { // sólo pone el año
            sb.append(date[0].trim());
        } else if (date.length == 2) { // primer campo mes, segundo campo año
            // cambiamos al formato año - mes
            sb.append(date[1].trim()).append('-').append(date[0].trim());
        } else if (date.length == 3) {
            // primer campo día, segundo campo mes y tercer campo año
            // cambiamos al formato año-mes-día
            sb.append(date[2].trim()).append('-').append(date[1].trim()).append('-').append(date[0].trim());
        }
        return sb.toString(); // retorna el string creado con la fecha.
    }
}
