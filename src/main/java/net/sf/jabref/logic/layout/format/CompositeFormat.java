/*  Copyright (C) 2003-2016 JabRef contributors.
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

import java.util.Arrays;
import java.util.List;

import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * A layout formatter that is the composite of the given Formatters executed in
 * order.
 *
 */
public class CompositeFormat implements LayoutFormatter {

    private List<LayoutFormatter> formatters;


    /**
     * If called with this constructor, this formatter does nothing.
     */
    public CompositeFormat() {
        // Nothing
    }

    public CompositeFormat(LayoutFormatter first, LayoutFormatter second) {
        formatters = Arrays.asList(first, second);
    }

    public CompositeFormat(LayoutFormatter[] formatters) {
        this.formatters = Arrays.asList(formatters);
    }

    @Override
    public String format(String fieldText) {
        String result = fieldText;
        if (formatters != null) {
            for (LayoutFormatter formatter : formatters) {
                result = formatter.format(result);
            }
        }
        return result;
    }

}
