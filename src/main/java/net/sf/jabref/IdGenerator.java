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

import java.text.NumberFormat;

/**
 * IDs are at least 8 digit long. The lowest ID is 00000000, the next would be 00000001.
 * <p/>
 * The generator is thread safe!
 */
public class IdGenerator {

    private static final NumberFormat idFormat;

    static {
        idFormat = NumberFormat.getInstance();
        IdGenerator.idFormat.setMinimumIntegerDigits(8);
        IdGenerator.idFormat.setGroupingUsed(false);
    }

    private static int idCounter = 0;

    public static synchronized String next() {
        String result = idFormat.format(idCounter);
        idCounter++;
        return result;
    }

    public static int getMinimumIntegerDigits() {
        return idFormat.getMinimumIntegerDigits();
    }
}

