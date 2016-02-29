/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * Formatter used to signal that a formatter hasn't been found. This can be
 * used for graceful degradation if a layout uses an undefined format.
 */
public class NotFoundFormatter implements LayoutFormatter {

    private final String notFound;


    public NotFoundFormatter(String notFound) {

        this.notFound = notFound;
    }

    public String getNotFound() {
        return notFound;
    }

    @Override
    public String format(String fieldText) {
        return '[' + Localization.lang("Formatter not found: %0", notFound) + "] " + fieldText;
    }
}
