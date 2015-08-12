/*
 * Copyright (C) 2003 Nathan Dunn.
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
package net.sf.jabref.logic.search;

import net.sf.jabref.BibtexEntry;

public interface SearchRule {

    /*
     * Because some rules require the query in the constructor,
     * the parameter query is not always used as expected.
     * The two constants provide means to mark this as dummy.
     * As I am not sure whether null could be substituted by "dummy" I leave everything as is.
     */
    String DUMMY_QUERY = "dummy";
    String NULL_QUERY = null;

    boolean applyRule(String query, BibtexEntry bibtexEntry);

    boolean validateSearchStrings(String query);
}
