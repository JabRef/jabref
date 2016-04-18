/*
 * iso690FormatDate.java
 *
 * Creado el 10 de septiembre de 2009, 16:07
 *
 * Copyright 2009, Laura Hernández Gómez
 *
 * This file is part of Plugin ISO 690.
 *
 *  Plugin ISO 690 is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plugin ISO 690 is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Plugin ISO 690.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.*;

/**
 *
 * @author Usuario
 */
public class Iso690FormatDate implements LayoutFormatter {

    @Override
    public String format(String s) {

        if (s == null || s.trim().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String[] date = s.split("de");
        //parte el string en los distintos campos de la fecha
        if (date.length == 1) { //sólo pone el año
            sb.append(date[0].trim());
        } else if (date.length == 2) {//primer campo mes, segundo campo año
            //cambiamos al formato año - mes
            sb.append(date[1].trim()).append('-').append(date[0].trim());
        } else if (date.length == 3) {
            //primer campo día, segundo campo mes y tercer campo año
            // cambiamos al formato año-mes-día
            sb.append(date[2].trim()).append('-').append(date[1].trim()).append('-').append(date[0].trim());
        }
        return sb.toString();//retorna el string creado con la fecha.
    }
}