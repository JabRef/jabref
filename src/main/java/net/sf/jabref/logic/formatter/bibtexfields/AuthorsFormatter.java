/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.model.entry.AuthorList;

/**
 * Class for normalizing author lists to BibTeX format.
 */
public class AuthorsFormatter implements Formatter {

    @Override
    public String getName() {
        return "BibTex authors format";
    }

    @Override
    public String getKey() {
        return "AuthorsFormatter";
    }

    /**
     *
     */
    @Override
    public String format(String value) {
        // try to convert to BibTeX format, where multiple names are separated by " and " instead of ";" or other characters
        String inputForAuthorList = value.replaceAll(";", " and ");

        // AuthorList does the whole magic when the string is a well-formed BibTeX author string
        AuthorList list = AuthorList.getAuthorList(inputForAuthorList);
        return list.getAuthorsLastFirstAnds(false);
    }

}
