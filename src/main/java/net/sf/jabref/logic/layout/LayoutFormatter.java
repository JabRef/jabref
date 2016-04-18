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
package net.sf.jabref.logic.layout;

/**
 * The LayoutFormatter is used for a Filter design-pattern.
 *
 * Implementing classes have to accept a String and returned a formatted version of it.
 *
 * Example:
 *
 *   "John von Neumann" => "von Neumann, John"
 *
 * @version 1.2 - Documentation CO
 */
@FunctionalInterface
public interface LayoutFormatter {

    /**
     * Failure Mode:
     * <p>
     * Formatters should be robust in the sense that they always return some
     * relevant string.
     * <p>
     * If the formatter can detect an invalid input it should return the
     * original string otherwise it may simply return a wrong output.
     *
     * @param fieldText
     *            The text to layout.
     * @return The layouted text.
     */
    String format(String fieldText);
}
