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
package net.sf.jabref.logic.search;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Every Listener that wants to receive events from a search needs to
 * implement this interface
 *
 * @author Ben
 *
 */
@FunctionalInterface
public interface SearchQueryHighlightListener {

    /**
     * Pattern with which one can determine what to highlight
     *
     * @param words null if nothing is searched for
     */
    void highlightPattern(Optional<Pattern> highlightPattern);
}
