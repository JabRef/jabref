/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EasyDateFormat {

    /**
     * The formatter objects
     */
    private SimpleDateFormat dateFormatter = null;

    /**
     * Creates a String containing the current date (and possibly time),
     * formatted according to the format set in preferences under the key
     * "timeStampFormat".
     *
     * @return The date string.
     */
    public String getCurrentDate() {
        return getDateAt(new Date());
    }

    /**
     * Creates a readable Date string from the parameter date. The format is set
     * in preferences under the key "timeStampFormat".
     *
     * @return The formatted date string.
     */
    public String getDateAt(Date date) {
        // first use, create an instance
        if (dateFormatter == null) {
            String format = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FORMAT);
            dateFormatter = new SimpleDateFormat(format);
        }
        return dateFormatter.format(date);
    }
}
